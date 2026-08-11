package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.IEntregaChofer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sincronización de {@code trch_Entregas} contra SAP, sacada del camino crítico del chofer.
 *
 * <h3>El problema que resuelve (medido, no estimado)</h3>
 * Hasta ahora, cada {@code POST /entregas/chofer-entrega} ejecutaba
 * {@code p_list_trch_Entregas @ACCION='A'}, y esa acción hace <b>dos</b> cosas antes de devolver
 * nada: primero llama a {@code p_abm_trch_Entregas @ACCION='A'}, que sincroniza la tabla entera
 * contra SAP por el linked server, y recién después filtra los pendientes de <i>un</i> chofer.
 *
 * <p>La sincronización cuesta <b>~1,4 s en caliente</b> (mínimo medido 1.145 ms, máximo 2.129 ms),
 * devuelve 417 filas de 30 días y 4 empresas, toca 490.427 páginas y pide 281 MB de memoria por
 * ejecución. El chofer que abre la app dispara la sincronización de <b>todos</b> los choferes de
 * <b>todas</b> las empresas; lo suyo son unas pocas filas. Con 15 choferes abriendo la app a la
 * misma hora eran 15 sincronizaciones idénticas y simultáneas, todas dentro del request HTTP, y
 * cada refresh volvía a pagarlas.
 *
 * <h3>Qué hace este servicio</h3>
 * <ol>
 *   <li><b>Cachea en el tiempo.</b> Recuerda cuándo fue el último intento de sincronización. Si
 *       pasaron menos de {@code entregas.sync.intervalo-segundos}, no sincroniza y vuelve
 *       enseguida: la tabla ya está fresca, no hay nada que hacer.</li>
 *   <li><b>Deja pasar un solo hilo.</b> El candado es un {@link AtomicBoolean} y se toma con
 *       {@code compareAndSet}, <b>sin esperar</b>. El hilo que lo gana sincroniza; los demás
 *       siguen de largo y leen lo que haya en la tabla. Esto es lo importante y es deliberado:
 *       <b>encolar a los otros 14 choferes detrás del que sincroniza sería volver exactamente al
 *       problema original</b>, solo que en serie en vez de en paralelo. Nadie espera nunca a
 *       nadie.</li>
 *   <li><b>Nunca lanza.</b> Si SAP está caído, se loguea y el chofer igual recibe su lista con lo
 *       último que se haya sincronizado. Datos de hace tres minutos son infinitamente mejores que
 *       un error en la cara de alguien que está en la calle con mala señal.</li>
 * </ol>
 *
 * <h3>Por qué el reloj mira el INTENTO y no el ÉXITO</h3>
 * Se guardan los dos instantes, pero el que decide si corresponde sincronizar es el del
 * <b>intento</b> ({@link #ultimoIntentoNanos}), no el del último éxito. Si se mirara el éxito, con
 * SAP caído la condición "hace más de N segundos que no sincronizo" sería verdadera <i>siempre</i>:
 * cada request volvería a tomar el candado y a esperar el timeout completo contra un SAP que no
 * contesta. El chofer terminaría esperando más que antes, justo cuando todo está peor. Mirando el
 * intento, un fallo también consume el intervalo y el reintento sale recién en la próxima ventana.
 * El instante del último éxito queda para saber qué antigüedad tienen los datos que se están
 * sirviendo, que es lo que uno quiere leer en el log cuando algo anda mal.
 *
 * <h3>Recomendación: agregar un {@code @Scheduled} y que nadie pague nunca</h3>
 * Este servicio elimina el costo <i>repetido</i>, pero no el primero: con la app recién levantada,
 * el primer chofer del día encuentra el intervalo vencido, gana el candado y paga el 1,4 s por
 * todos. Eso se arregla sincronizando desde el scheduler, <b>antes</b> de que alguien abra la app.
 *
 * <p>Concretamente, en {@code scheduler.DatabaseTaskScheduler} (que ya existe, ya es
 * {@code @Component} y el proyecto ya tiene {@code @EnableScheduling} en {@code BosqueApplication}):
 *
 * <pre>{@code
 * // inyectar SincronizacionEntregasService en el constructor y:
 *
 * @Scheduled(fixedDelay = 120000L, initialDelay = 30000L)
 * public void sincronizarEntregasConSap() {
 *     sincronizacionEntregasService.sincronizarAhora("programada");
 * }
 * }</pre>
 *
 * <p>Tres detalles de esa configuración que no son arbitrarios:
 * <ul>
 *   <li><b>{@code fixedDelay} y no {@code fixedRate}.</b> {@code fixedDelay} cuenta desde que
 *       <i>terminó</i> la ejecución anterior, así que una sincronización lenta nunca se solapa ni
 *       acumula ejecuciones atrasadas. Con {@code fixedRate}, un SAP degradado dejaría corridas
 *       encoladas — y el pool del scheduler de Spring es de <b>un solo hilo</b>, compartido con
 *       los tres jobs que ya viven en {@code DatabaseTaskScheduler}.</li>
 *   <li><b>2 minutos contra un intervalo de 3.</b> El período programado tiene que ser MENOR que
 *       {@code entregas.sync.intervalo-segundos}. Así, cuando llega el request de un chofer, el
 *       intervalo casi nunca está vencido: la tabla ya la refrescó el scheduler y el chofer solo
 *       hace su {@code SELECT}. La sincronización dentro del request pasa a ser una red de
 *       seguridad que casi no se activa, en vez de el camino normal.</li>
 *   <li><b>{@code initialDelay}.</b> Para no pelearle el arranque al resto del contexto de Spring.</li>
 * </ul>
 *
 * <p>{@link #sincronizarAhora(String)} es justamente el método pensado para eso: ignora el
 * intervalo (el scheduler ya <i>es</i> el intervalo) pero respeta el mismo candado, así que un job
 * programado y un request de chofer no pueden sincronizar a la vez. Y como el job actualiza el
 * mismo reloj, después de cada corrida programada los choferes ven el intervalo fresco y ni
 * siquiera evalúan sincronizar.
 *
 * <h3>El interruptor de emergencia</h3>
 * {@code entregas.sync.habilitado=false} <b>no</b> deja al chofer sin sincronizar: hace que el
 * controlador vuelva al camino histórico ({@code ACCIÓN 'A'}, sincroniza dentro de cada request).
 * Es la vuelta atrás completa, sin recompilar, para el caso de que el script SQL de las acciones
 * nuevas todavía no se haya corrido en producción.
 */
@Service
public class SincronizacionEntregasService {

    private static final Logger logger = LoggerFactory.getLogger(SincronizacionEntregasService.class);

    /** Valor que se usa si la propiedad viene en 0, negativa o ausente. */
    private static final int INTERVALO_POR_DEFECTO = 180;

    /**
     * Piso duro del intervalo. Un valor chiquito convierte esto de vuelta en "sincronizar en cada
     * request", que es el problema que vino a resolver. Para eso está
     * {@code entregas.sync.habilitado=false}, que además es explícito.
     */
    private static final int INTERVALO_MINIMO = 15;

    private static final long NANOS_POR_SEGUNDO = 1000000000L;

    private final IEntregaChofer entregaChoferDao;

    /**
     * Interruptor general de la sincronización diferida.
     *
     * <p>En {@code false} el controlador NO usa este servicio y vuelve al camino histórico
     * (ACCIÓN 'A': sincroniza dentro de cada request del chofer). Es la vuelta atrás completa.
     */
    @Value("${entregas.sync.habilitado:true}")
    private boolean habilitado;

    /** Cada cuántos segundos, como máximo, se vuelve a sincronizar con SAP. */
    @Value("${entregas.sync.intervalo-segundos:180}")
    private int intervaloSegundos;

    /** El intervalo ya validado y pasado a nanos. Se calcula una vez en el arranque. */
    private long intervaloNanos;

    /**
     * El candado. {@code true} significa "hay un hilo sincronizando ahora mismo".
     *
     * <p>Se toma con {@code compareAndSet(false, true)} y nunca con espera. No es un
     * {@code synchronized} ni un {@code ReentrantLock.lock()} a propósito: acá el que no consigue
     * el candado tiene que <b>irse</b>, no formarse en la fila.
     */
    private final AtomicBoolean sincronizando = new AtomicBoolean(false);

    /**
     * {@code System.nanoTime()} del comienzo del último intento, exitoso o no.
     *
     * <p>Es {@code nanoTime} y no {@code currentTimeMillis} porque es un reloj monótono: un ajuste
     * de hora del sistema (NTP, cambio manual) no puede ni bloquear la sincronización por horas ni
     * dispararla de más. Para medir un intervalo, la hora de pared es el reloj equivocado.
     */
    private final AtomicLong ultimoIntentoNanos = new AtomicLong(0L);

    /** Hora de pared del último éxito, 0 si todavía no hubo ninguno. Solo para el log. */
    private final AtomicLong ultimaSincronizacionOkMs = new AtomicLong(0L);

    private final AtomicLong contadorOk = new AtomicLong(0L);
    private final AtomicLong contadorFallida = new AtomicLong(0L);
    private final AtomicLong contadorOmitidaPorIntervalo = new AtomicLong(0L);
    private final AtomicLong contadorOmitidaPorCandado = new AtomicLong(0L);

    public SincronizacionEntregasService(IEntregaChofer entregaChoferDao) {
        this.entregaChoferDao = entregaChoferDao;
    }

    /**
     * Valida el intervalo y deja en el log, al arrancar, en qué modo quedó la sincronización.
     *
     * <p>Un intervalo inválido no tumba el arranque: se corrige al valor por defecto y se avisa.
     * Quedarse sin backend porque alguien escribió mal un número en el {@code .properties} sería
     * mucho peor que sincronizar cada 3 minutos en vez de cada 5.
     */
    @PostConstruct
    void inicializar() {
        int efectivo = intervaloSegundos;
        if (efectivo <= 0) {
            logger.warn("entregas.sync.intervalo-segundos={} no es válido; se usa el valor por defecto de {} s.",
                        Integer.valueOf(intervaloSegundos), Integer.valueOf(INTERVALO_POR_DEFECTO));
            efectivo = INTERVALO_POR_DEFECTO;
        } else if (efectivo < INTERVALO_MINIMO) {
            logger.warn("entregas.sync.intervalo-segundos={} es demasiado chico y devolvería el problema que "
                      + "este servicio resuelve (una sincronización de ~1,4 s por cada refresh de cada chofer). "
                      + "Se eleva a {} s. Si lo que se busca es el comportamiento anterior, usar "
                      + "entregas.sync.habilitado=false, que es explícito.",
                        Integer.valueOf(intervaloSegundos), Integer.valueOf(INTERVALO_MINIMO));
            efectivo = INTERVALO_MINIMO;
        }
        this.intervaloSegundos = efectivo;
        this.intervaloNanos = (long) efectivo * NANOS_POR_SEGUNDO;

        // Deja el reloj en "vencido" para que la primera consulta sí sincronice, sin depender de
        // que nanoTime() arranque en cero (no lo garantiza: puede ser negativo).
        this.ultimoIntentoNanos.set(System.nanoTime() - intervaloNanos - 1L);

        if (!habilitado) {
            logger.warn("Sincronización diferida de entregas APAGADA (entregas.sync.habilitado=false): "
                      + "/entregas/chofer-entrega vuelve al camino histórico y sincroniza con SAP DENTRO de cada "
                      + "request (~1,4 s por request, por chofer, por refresh).");
        } else {
            logger.info("Sincronización diferida de entregas ACTIVA: se sincroniza con SAP como máximo una vez "
                      + "cada {} s y un solo hilo a la vez. Para que ni el primer chofer del día espere, agregar "
                      + "un @Scheduled(fixedDelay = {} ms) que llame a sincronizarAhora(); ver el javadoc de "
                      + "SincronizacionEntregasService.",
                        Integer.valueOf(efectivo), Long.valueOf((long) efectivo * 1000L * 2L / 3L));
        }
    }

    /**
     * Deja la tabla razonablemente al día antes de que el chofer la lea. <b>Nunca lanza y nunca
     * espera a otro hilo.</b>
     *
     * <p>Vuelve enseguida —sin tocar la base— en los dos casos frecuentes:
     * <ul>
     *   <li>el intervalo todavía no venció: la tabla ya está fresca;</li>
     *   <li>otro hilo está sincronizando en este preciso momento: el candado está tomado y este
     *       request sigue de largo. Va a leer datos de hasta un intervalo de antigüedad, que es
     *       exactamente el trato.</li>
     * </ul>
     *
     * <p>Solo el hilo que encuentra el intervalo vencido <i>y</i> gana el candado paga la
     * sincronización, y la paga completa: cuando este método vuelve, si fue él quien sincronizó,
     * los datos nuevos ya están comprometidos en la tabla. Por eso el controlador lee
     * <b>después</b> de llamar acá y no antes.
     */
    public void asegurarSincronizado() {
        try {
            if (!habilitado) {
                return;
            }
            if (!intervaloVencido()) {
                contadorOmitidaPorIntervalo.incrementAndGet();
                return;
            }
            if (!sincronizando.compareAndSet(false, true)) {
                // Otro hilo está adentro. No se espera: se lee lo que haya. Esperar acá sería
                // poner a los 14 choferes restantes en fila detrás de una consulta a SAP.
                contadorOmitidaPorCandado.incrementAndGet();
                logger.debug("Ya hay una sincronización con SAP en curso; este request no espera y lee la tabla "
                           + "tal como está.");
                return;
            }
            try {
                // Segunda mirada, ya con el candado en la mano: entre el chequeo de arriba y el
                // compareAndSet pudo haber terminado otro hilo. Sin esto, dos requests casi
                // simultáneos con el intervalo recién vencido harían dos sincronizaciones seguidas.
                if (!intervaloVencido()) {
                    contadorOmitidaPorIntervalo.incrementAndGet();
                    return;
                }
                ejecutarSincronizacion("request de chofer");
            } finally {
                sincronizando.set(false);
            }
        } catch (Throwable t) {
            // Techo absoluto. Este método se llama desde el hilo del request del chofer y no puede
            // ser jamás el motivo de un 500: en el peor caso el chofer ve datos viejos.
            logger.error("Error inesperado al asegurar la sincronización de entregas: {}", t.getMessage(), t);
        }
    }

    /**
     * Sincroniza ahora, sin mirar el intervalo. Pensado para el {@code @Scheduled} recomendado en
     * el javadoc de la clase y para un eventual endpoint de mantenimiento.
     *
     * <p>Sí respeta el candado: si un request de chofer está sincronizando en este momento, esta
     * llamada no hace nada y devuelve {@code false}. Es lo correcto — el trabajo que iba a hacer
     * ya lo está haciendo alguien.
     *
     * <p>Al actualizar el mismo reloj que mira {@link #asegurarSincronizado()}, cada corrida
     * programada le "regala" un intervalo entero de calma a los requests de los choferes.
     *
     * <p>Nunca lanza.
     *
     * @param motivo texto corto para el log ("programada", "manual", ...); puede ser {@code null}
     * @return {@code true} solo si esta llamada sincronizó y SAP respondió bien
     */
    public boolean sincronizarAhora(String motivo) {
        try {
            if (!habilitado) {
                logger.debug("Sincronización con SAP pedida ({}) pero entregas.sync.habilitado=false; se omite.",
                             motivo);
                return false;
            }
            if (!sincronizando.compareAndSet(false, true)) {
                contadorOmitidaPorCandado.incrementAndGet();
                logger.info("Sincronización con SAP pedida ({}) mientras ya había una en curso; se omite esta.",
                            motivo);
                return false;
            }
            try {
                return ejecutarSincronizacion(motivo == null ? "manual" : motivo);
            } finally {
                sincronizando.set(false);
            }
        } catch (Throwable t) {
            logger.error("Error inesperado al sincronizar entregas con SAP ({}): {}", motivo, t.getMessage(), t);
            return false;
        }
    }

    /**
     * ¿Está activa la sincronización diferida? El controlador lo usa para elegir entre el camino
     * nuevo (ACCIÓN 'Y', leer sin sincronizar) y el histórico (ACCIÓN 'A', sincronizar y leer).
     */
    public boolean isHabilitado() {
        return habilitado;
    }

    /** Intervalo efectivo en segundos, ya validado. Para logs y diagnósticos. */
    public int getIntervaloSegundos() {
        return intervaloSegundos;
    }

    /**
     * Hace cuántos segundos que la tabla se sincronizó bien con SAP, o {@code -1} si todavía no
     * hubo ninguna sincronización exitosa desde que arrancó la aplicación.
     *
     * <p>Es la antigüedad de los datos que están viendo los choferes.
     */
    public long segundosDesdeUltimaSincronizacionOk() {
        long ok = ultimaSincronizacionOkMs.get();
        if (ok == 0L) {
            return -1L;
        }
        return (System.currentTimeMillis() - ok) / 1000L;
    }

    // =====================================================================
    // Internos
    // =====================================================================

    /**
     * ¿Pasó el intervalo desde que empezó el último intento?
     *
     * <p>La resta de dos {@code nanoTime()} es la forma correcta de comparar instantes monótonos:
     * es inmune al desbordamiento del contador.
     */
    private boolean intervaloVencido() {
        return (System.nanoTime() - ultimoIntentoNanos.get()) >= intervaloNanos;
    }

    /**
     * Corre la sincronización de verdad. Se entra acá <b>siempre</b> con el candado tomado.
     *
     * <p>El reloj del intento se pisa ANTES de llamar a la base, no después. Mientras la
     * sincronización esté corriendo —y son más de mil milisegundos— el resto de los requests ven
     * el intervalo fresco y ni se acercan al candado. Y si SAP falla, el fallo también consume el
     * intervalo: el próximo intento sale en la siguiente ventana en vez de en el request siguiente.
     */
    private boolean ejecutarSincronizacion(String motivo) {
        ultimoIntentoNanos.set(System.nanoTime());
        long inicio = System.currentTimeMillis();

        boolean ok;
        try {
            ok = entregaChoferDao.sincronizarConSap();
        } catch (Throwable t) {
            // El DAO promete no lanzar. Este catch existe igual: si esa promesa se rompiera
            // alguna vez, el que se lleva la excepción es el chofer que está mirando la pantalla.
            logger.error("La sincronización con SAP ({}) lanzó una excepción inesperada: {}",
                         motivo, t.getMessage(), t);
            ok = false;
        }

        long duracion = System.currentTimeMillis() - inicio;

        if (ok) {
            ultimaSincronizacionOkMs.set(System.currentTimeMillis());
            long total = contadorOk.incrementAndGet();
            logger.info("Entregas sincronizadas con SAP ({}) en {} ms. Próxima recién dentro de {} s. "
                      + "[ok={}, fallidas={}, omitidas por intervalo={}, omitidas por candado={}]",
                        motivo, Long.valueOf(duracion), Integer.valueOf(intervaloSegundos),
                        Long.valueOf(total), Long.valueOf(contadorFallida.get()),
                        Long.valueOf(contadorOmitidaPorIntervalo.get()),
                        Long.valueOf(contadorOmitidaPorCandado.get()));
        } else {
            contadorFallida.incrementAndGet();
            long antiguedad = segundosDesdeUltimaSincronizacionOk();
            logger.warn("No se pudo sincronizar entregas con SAP ({}) después de {} ms. Los choferes siguen "
                      + "viendo {}. No se reintenta hasta dentro de {} s. [ok={}, fallidas={}]",
                        motivo, Long.valueOf(duracion),
                        antiguedad < 0
                            ? "lo que hubiera quedado en la tabla de la ejecución anterior de la aplicación"
                            : "datos de hace " + antiguedad + " s",
                        Integer.valueOf(intervaloSegundos),
                        Long.valueOf(contadorOk.get()), Long.valueOf(contadorFallida.get()));
        }
        return ok;
    }
    /**
     * ¿Se puede usar la lectura sin sincronización en esta base?
     *
     * <p>Dos condiciones, y las dos se resuelven UNA sola vez: que el flag de configuración esté
     * encendido, y que el script SQL esté efectivamente corrido acá. Lo segundo se le pregunta al
     * catálogo de la base — no se deduce de que una consulta haya venido vacía.
     *
     * <p><b>Por qué importa la diferencia.</b> Estos SP son una cadena de {@code IF @ACCION = ...}:
     * una acción inexistente no falla, devuelve cero filas. Cero filas es también lo que devuelve
     * un chofer sin pendientes. Si se infiere "la acción anda" de "vino vacía", basta que el primer
     * chofer tras el arranque no tenga trabajo para que la conclusión quede fijada mal y TODOS los
     * choferes vean la lista vacía, sin un solo error en el log, hasta reiniciar la JVM.
     *
     * <h3>Por qué el "sí" se cachea para siempre y el "no" se reintenta</h3>
     * La primera versión cacheaba las dos respuestas por igual, y eso tenía un agujero que se
     * materializó el 11/08: la JVM arrancó 09:14, el script SQL se corrió 09:44, y cualquier
     * consulta de un chofer en esos 30 minutos dejaba el "no" fijado <b>para toda la vida del
     * proceso</b>. El script quedaba desplegado, la optimización lista, y el backend siguiendo
     * por el camino lento sin un solo error nuevo en el log. Nadie tenía forma de notarlo.
     *
     * <p>Las dos respuestas no son simétricas y por eso no se tratan igual:
     * <ul>
     *   <li><b>{@code true}</b> se cachea sin vencimiento. Un SP desplegado no desaparece a
     *       mitad del día, y si alguien lo borrara, el camino histórico sigue funcionando.</li>
     *   <li><b>{@code false}</b> vale sólo {@value #REINTENTO_SOPORTE_MS} ms. "El script todavía
     *       no está corrido" es una condición <i>transitoria</i>: se arregla justamente
     *       corriendo el script. Reintentar hace que el despliegue se tome solo, sin reiniciar
     *       nada.</li>
     * </ul>
     *
     * <p>El costo del reintento es una consulta al catálogo por minuto en el peor caso —
     * un {@code OBJECT_ID} contra la caché de metadatos— y sólo mientras el script no esté.
     */
    public boolean soportaLecturaSinSync() {
        if (!habilitado) {
            return false;
        }

        // Un "sí" ya confirmado no se vuelve a preguntar nunca.
        if (this.soporteConfirmado) {
            return true;
        }

        // Un "no" se reintenta cuando venció. Fuera del synchronized para no serializar el
        // camino caliente: mientras el soporte no exista, esto son unos pocos requests por
        // minuto llegando a la comprobación, no todos.
        long ahora = System.currentTimeMillis();
        if (ahora - this.ultimoIntentoSoporte < REINTENTO_SOPORTE_MS) {
            return false;
        }

        synchronized (this) {
            if (this.soporteConfirmado) {
                return true;
            }
            if (System.currentTimeMillis() - this.ultimoIntentoSoporte < REINTENTO_SOPORTE_MS) {
                return false;
            }
            this.ultimoIntentoSoporte = System.currentTimeMillis();

            boolean hay = this.entregaChoferDao.existeSoporteSyncDesacoplada();
            if (hay) {
                this.soporteConfirmado = true;
                logger.info("Lectura sin sincronización HABILITADA: el script está desplegado. "
                          + "A partir de ahora los choferes leen sin esperar a SAP.");
            }
            return hay;
        }
    }

    /** Cuánto vale un "todavía no está el script" antes de volver a preguntar. */
    private static final long REINTENTO_SOPORTE_MS = 60_000L;

    /** {@code true} en cuanto el catálogo confirma el soporte. No vuelve a false. */
    private volatile boolean soporteConfirmado;

    /** Instante del último chequeo fallido, para no preguntar en cada request. */
    private volatile long ultimoIntentoSoporte;

}
