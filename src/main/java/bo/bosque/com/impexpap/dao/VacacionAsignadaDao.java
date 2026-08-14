package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.config.SpConfirmableException;
import bo.bosque.com.impexpap.config.SpConflictException;
import bo.bosque.com.impexpap.model.VacacionAsignada;
import bo.bosque.com.impexpap.utils.Fmt;
import bo.bosque.com.impexpap.utils.SpEscritura;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Escrituras y lecturas de {@code trh_vacacionAsignada}.
 *
 * <h3>Estilo legacy y no {@code SpHelper.ejecutarAbmMap}</h3>
 * {@code ejecutarAbmMap} agrega a ciegas {@code @error/@errormsg/@idGenerado OUTPUT} y
 * {@code p_abm_vacacionAsignada} (de 2016) no declara ninguno de los tres: el {@code EXEC}
 * fallaría. Se escribe con {@code jdbcTemplate.update} sobre el SP tal como está, que es lo que
 * hace hoy el ERP. El molde limpio es {@link CombustibleControlDao}, no {@link ColorDao} — ése
 * tiene un {@code @ACCION} que nunca llega al SQL y un {@code this.jdbcTemplate = null} en el
 * catch que deja el DAO muerto hasta reiniciar la aplicación.
 *
 * <h3>Un SQL por ACCION</h3>
 * Cada operación manda SÓLO los parámetros que esa ACCION usa de verdad, por nombre. El UPDATE ni
 * siquiera nombra a {@code @codEmpleado} ni a {@code @codRelEmplEmpr} porque el SP no los
 * actualiza, y así no queda un parámetro que parezca que hace algo.
 *
 * <h3>Qué devuelve el rowcount</h3>
 * Los tres SP llevan {@code SET NOCOUNT OFF}, así que el conteo llega al driver — pero los
 * triggers {@code dai_/dau_/dad_} no lo apagan y emiten 5 o 6 conteos extra por operación. El
 * número que vuelve sirve para "algo pasó / no pasó" y para nada más. <b>Para saber qué quedó, se
 * relee.</b>
 */
@Slf4j
@Repository
public class VacacionAsignadaDao implements IVacacionAsignada {

    private static final String SP_LIST = "p_list_vacacionAsignada";

    private static final String SQL_ALTA =
            "execute p_abm_vacacionAsignada "
          + "@codEmpleado=?, @diasAsignados=?, @motivo=?, @fecha=?, "
          + "@codRelEmplEmpr=?, @audUsuarioI=?, @ACCION=?";

    /** El SP sólo pisa días, motivo, fecha y auditoría; lo demás no viaja. */
    private static final String SQL_EDICION =
            "execute p_abm_vacacionAsignada "
          + "@codVacacionAsignada=?, @diasAsignados=?, @motivo=?, @fecha=?, "
          + "@audUsuarioI=?, @ACCION=?";

    private static final String SQL_BAJA =
            "execute p_abm_vacacionAsignada @codVacacionAsignada=?, @ACCION=?";

    /** Largo de {@code trh_vacacionAsignada.motivo}. Uno más y el INSERT revienta por truncamiento. */
    static final int MOTIVO_MAX = 300;

    /**
     * Topes de cordura, <b>no reglas de negocio</b>: el SP acepta cualquier número. Medido en la
     * tabla, todo lo real vive entre 0 y 30 días. Por encima de {@link #DIAS_INUSUAL} se pide
     * confirmación explícita y por encima de {@link #DIAS_TOPE} se rechaza, porque a esa altura es
     * un error de tipeo y son días pagados.
     */
    static final double DIAS_INUSUAL = 30;
    static final double DIAS_TOPE    = 60;

    /**
     * Ventana del doble toque. Una fila IDÉNTICA (mismos días, mismo motivo, misma fecha, misma
     * persona-relación) cargada hace menos de esto no es una decisión: es el botón apretado dos
     * veces con la red a medias. Se rechaza <b>aunque el usuario confirme</b>, porque la
     * confirmación viaja igual en el segundo toque.
     */
    static final long VENTANA_DOBLE_TOQUE_MS = 2L * 60 * 1000;

    private final SpHelper spHelper;
    private final JdbcTemplate jdbcTemplate;

    public VacacionAsignadaDao(SpHelper spHelper, JdbcTemplate jdbcTemplate) {
        this.spHelper = spHelper;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ══════════════════════════════════════════════════════════════════════
    // LECTURAS
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<VacacionAsignada> historial(long codEmpleado, Long codRelEmplEmpr, Date fechaCorte) {
        // Sin fecha, la ACCION 'B' devuelve la grilla VACÍA y no un error: su WHILE es
        // `@fechaBase <= @fecha` y el filtro de las filas reales es `fecha <= @fecha`. Con NULL las
        // dos condiciones dan falso y el empleado parece no tener ni un aniversario.
        Date corte = fechaCorte != null ? fechaCorte : new Date();
        // Y sin relación, ídem: se resuelve la vigente acá en vez de devolver un 400 que obliga a
        // la pantalla a consultar la ficha primero. Misma consulta que usa el alta.
        long codRel = (codRelEmplEmpr != null && codRelEmplEmpr > 0)
                ? codRelEmplEmpr
                : AbonoDiasDao.relacionActivaDe(jdbcTemplate, codEmpleado);

        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);
        filtro.put("codRelEmplEmpr", codRel);
        filtro.put("fecha", new java.sql.Date(corte.getTime()));

        List<VacacionAsignada> filas = spHelper.ejecutarListado(SP_LIST, filtro, "B", VacacionAsignada.class);
        quitarSinteticasYaRegistradas(filas);
        filas.forEach(this::decorar);
        return filas;
    }

    /**
     * Saca las filas "sin registrar" que en realidad <b>sí</b> están registradas.
     *
     * <h3>El bug del SP</h3>
     * El {@code WHILE} de la ACCION 'B' avanza de aniversario en aniversario desde la
     * {@code fechaIni} de la relación de beneficios y, por cada uno que "no tiene registro",
     * inventa una fila con {@code codVacacionAsignada = 0}. Para decidir si tiene registro compara
     * <b>igualdad exacta de fecha</b>: {@code where tt.fecha = @fechaBase}.
     *
     * <p>Y las filas reales no están en el aniversario: están en <b>el día anterior al aniversario
     * siguiente</b>, porque la asignación se acredita al <i>cumplir</i> el año, no al empezarlo.
     * Medido sobre el empleado 213 (relación 457, beneficio 01/06/2022), sus tres asignaciones
     * reales son 31/05/2023, 31/05/2024 y 31/05/2025, mientras el bucle prueba 01/06/2022,
     * 01/06/2023, 01/06/2024 y 01/06/2025: <b>no coincide una sola vez</b>. En toda la tabla el
     * desfase es sistemático — 637 filas caen en día 31 y 170 en día 30, contra 74 en día 1 sobre
     * 1.424, mientras 84 de los 85 empleados activos arrancan el beneficio en día 1.
     *
     * <p>Resultado en pantalla: un «Sin registrar» pegado a cada fila real, diciéndole a RR.HH.
     * que no se asignó nada en años donde sí se asignó, y ofreciendo un alta que duplicaría.
     *
     * <h3>El emparejamiento</h3>
     * Una sintética fechada en el aniversario {@code A} cubre el año laboral {@code (A, A+1año]},
     * y la fila real de ese año cae dentro de esa ventana (típicamente en {@code A+1año-1día}). Si
     * hay una real ahí, la sintética sobra. La ventana es <b>abierta a la izquierda</b> para que
     * una fila fechada justo en un aniversario pertenezca a un solo año y no tape dos huecos.
     *
     * <p><b>Las sintéticas que quedan son las de verdad:</b> años cumplidos a los que efectivamente
     * nunca se les cargó la asignación. Ahí el alta corresponde, y es el único lugar donde la
     * pantalla la ofrece.
     *
     * <p>Se corrige acá y no en el SP porque {@code p_list_vacacionAsignada} lo comparte el ERP
     * viejo, que sigue vivo: cambiarlo movería la grilla de {@code permiso.xhtml} en la misma
     * jugada. El arreglo de fondo —que el {@code WHILE} compare por año— sigue en el backlog del
     * DBA (plan §D3, prioridad media).
     */
    static void quitarSinteticasYaRegistradas(List<VacacionAsignada> filas) {
        List<Date> reales = new ArrayList<>();
        for (VacacionAsignada v : filas) {
            if (v.getCodVacacionAsignada() != 0 && v.getFecha() != null) reales.add(v.getFecha());
        }
        if (reales.isEmpty()) return;

        Iterator<VacacionAsignada> it = filas.iterator();
        while (it.hasNext()) {
            VacacionAsignada v = it.next();
            if (v.getCodVacacionAsignada() != 0 || v.getFecha() == null) continue;
            if (hayRealEnElAnioQueAbre(v.getFecha(), reales)) it.remove();
        }
    }

    /** ¿Alguna fila real cae en {@code (aniversario, aniversario + 1 año]}? */
    private static boolean hayRealEnElAnioQueAbre(Date aniversario, List<Date> reales) {
        Calendar c = Calendar.getInstance();
        c.setTime(aniversario);
        c.add(Calendar.YEAR, 1);
        Date cierre = c.getTime();
        for (Date real : reales) {
            if (real.after(aniversario) && !real.after(cierre)) return true;
        }
        return false;
    }

    /**
     * ACCION 'D' — "Buscar Vac Ganadas" de la nómina de permisos: las vacaciones asignadas del
     * empleado <b>entre dos fechas</b>, tal como están en la tabla.
     *
     * <p><b>No es {@link #historial}</b> y por eso es otro método: la ACCION 'B' arma el desglose
     * por aniversario e <i>inventa</i> filas sintéticas ({@code codVacacionAsignada = 0}) para los
     * años que todavía no se cargaron, mientras que la 'D' es un SELECT pelado sobre
     * {@code trh_vacacionAsignada} ordenado por fecha DESC. La grilla del legacy
     * ({@code dtListBusqVacEmp}) muestra justamente las filas reales.
     *
     * <p>Las dos fechas son opcionales: el SP compara {@code fecha >= @fecha} y
     * {@code fecha <= @fechaBase}, así que sin ninguna devuelve todo el historial. El legacy manda
     * siempre {@code @diasAsignados} en 0, que su propio {@code (@x IS NULL OR @x = col)} traduce a
     * "sólo las de cero días" — acá directamente no se manda.
     */
    @Override
    public List<VacacionAsignada> buscarPorRango(long codEmpleado, Long codRelEmplEmpr,
                                                 Date desde, Date hasta) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);
        long codRel = (codRelEmplEmpr != null && codRelEmplEmpr > 0)
                ? codRelEmplEmpr
                : AbonoDiasDao.relacionActivaDe(jdbcTemplate, codEmpleado);
        filtro.put("codRelEmplEmpr", codRel);
        if (desde != null) filtro.put("fecha",      new java.sql.Date(desde.getTime()));
        if (hasta != null) filtro.put("fechaBase",  new java.sql.Date(hasta.getTime()));

        List<VacacionAsignada> filas = spHelper.ejecutarListado(SP_LIST, filtro, "D", VacacionAsignada.class);
        filas.forEach(this::decorar);
        return filas;
    }

    @Override
    public VacacionAsignada porId(long codVacacionAsignada) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codVacacionAsignada", codVacacionAsignada);

        List<VacacionAsignada> filas = spHelper.ejecutarListado(SP_LIST, filtro, "L", VacacionAsignada.class);
        if (filas.isEmpty()) return null;
        return decorar(filas.get(0));
    }

    // ══════════════════════════════════════════════════════════════════════
    // ESCRITURAS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ACCION 'I'. <b>El SP no valida nada</b> —acepta días negativos, fechas de 1900, empleados
     * inexistentes hasta que corta la FK— así que las cinco reglas de abajo son la única defensa.
     * Van en el orden del legacy y cortan en el primer fallo.
     */
    @Override
    public VacacionAsignada alta(VacacionAsignada v, long audUsuarioI, boolean confirmado) {
        if (v == null || v.getCodEmpleado() <= 0) {
            throw new SpBusinessException("No se seleccionó al empleado.");
        }
        validarDias(v.getDiasAsignados(), confirmado);
        final String motivo = validarMotivo(v.getMotivo());
        if (v.getFecha() == null) {
            throw new SpBusinessException("Falta la fecha del aniversario a asignar.");
        }
        SpEscritura.exigirUsuarioAuditor(audUsuarioI);

        final long codEmpleado = v.getCodEmpleado();
        /*
         * LA RELACIÓN LA RESUELVE EL SERVIDOR, NO EL BODY.
         *
         * Antes se tomaba `v.getCodRelEmplEmpr()` tal cual venía y sólo se comprobaba que fuera
         * > 0 — o sea, NO se comprobaba que fuera del empleado. Un POST con la relación de otra
         * persona insertaba la vacación en la relación equivocada y le movía el saldo a dos
         * persona-relación de una vez, sin que ninguna validación chistara. Es la misma decisión
         * que ya tomaba el alta de abono, con la misma consulta.
         */
        final long codRel = AbonoDiasDao.relacionActivaDe(jdbcTemplate, codEmpleado);
        final Date fecha = v.getFecha();
        validarAniversarioLibre(codEmpleado, codRel, fecha, v.getDiasAsignados(), motivo, confirmado);

        ejecutar(SQL_ALTA, "dar de alta la vacación asignada", ps -> {
            ps.setLong(1, codEmpleado);
            ps.setDouble(2, v.getDiasAsignados());
            ps.setString(3, motivo);
            ps.setDate(4, new java.sql.Date(fecha.getTime()));
            ps.setLong(5, codRel);
            ps.setLong(6, audUsuarioI);
            ps.setString(7, "I");
        });

        log.info("Alta de vacacion asignada: empleado={}, relacion={}, fecha={}, dias={}, usuario={}",
                 codEmpleado, codRel, fecha, v.getDiasAsignados(), audUsuarioI);

        // El SP no devuelve SCOPE_IDENTITY: la única forma de saber qué quedó es releer.
        VacacionAsignada guardada = ultimaEn(codEmpleado, codRel, fecha);
        if (guardada == null) {
            throw new SpBusinessException(
                    "La vacación asignada no aparece en la base después de guardarla. "
                  + "Vuelva a consultar el desglose antes de reintentar.");
        }
        return guardada;
    }

    /** ACCION 'U'. Sólo días y motivo; la fecha y el dueño de la fila se conservan. */
    @Override
    public VacacionAsignada edicion(long codVacacionAsignada, double diasAsignados, String motivo,
                                    long audUsuarioI) {
        if (codVacacionAsignada <= 0) {
            throw new SpBusinessException("No se indicó qué vacación asignada editar.");
        }
        VacacionAsignada actual = porId(codVacacionAsignada);
        if (actual == null) {
            throw new SpBusinessException("No se encontró el registro; puede que ya lo hayan borrado.");
        }
        validarDias(diasAsignados, true);   // en la edición no hay confirmación previa que pedir
        final String motivoOk = validarMotivo(motivo);
        SpEscritura.exigirUsuarioAuditor(audUsuarioI);

        actualizar(codVacacionAsignada, diasAsignados, motivoOk, actual.getFecha(), audUsuarioI);

        log.info("Edicion de vacacion asignada {}: dias {} -> {}, usuario={}",
                 codVacacionAsignada, actual.getDiasAsignados(), diasAsignados, audUsuarioI);
        return porId(codVacacionAsignada);
    }

    /** ACCION 'D'. DELETE físico; el trigger {@code dad_} lo archiva antes de que desaparezca. */
    @Override
    public VacacionAsignada baja(long codVacacionAsignada, long codEmpleado, long audUsuarioI) {
        if (codVacacionAsignada <= 0) {
            throw new SpBusinessException("No se indicó qué vacación asignada dar de baja.");
        }
        VacacionAsignada actual = porId(codVacacionAsignada);
        if (actual == null) {
            throw new SpBusinessException("No se encontró el registro; puede que ya lo hayan borrado.");
        }
        // El SP borra por id sin mirar de quién es la fila: la pertenencia se comprueba acá.
        if (codEmpleado > 0 && actual.getCodEmpleado() != codEmpleado) {
            throw new SpBusinessException(
                    "Ese registro no es del empleado que está viendo; refresque la pantalla.");
        }
        SpEscritura.exigirUsuarioAuditor(audUsuarioI);

        /*
         * QUIÉN BORRÓ, ESCRITO EN LA FILA ANTES DE QUE LA FILA DESAPAREZCA.
         *
         * La ACCION 'D' no recibe @audUsuarioI: hace un DELETE pelado. El trigger
         * dad_vacacionAsignada archiva la fila en trh_vacacionAsignadaEliminado, pero con el
         * audUsuarioI que la fila TENÍA — el de quien la creó, o -1 si la puso el job mensual. O
         * sea que el archivo decía que la borró alguien que no fue.
         *
         * Se ejecuta la 'U' con LOS MISMOS VALORES y el usuario del token: nada cambia de verdad,
         * pero la fila queda firmada y el trigger la archiva con el autor correcto. No toca ningún
         * SP. Y como dau_ sólo escribe en tb_bitacora cuando el valor viejo difiere del nuevo, este
         * UPDATE no ensucia la auditoría con filas de "cambió de 15 a 15".
         */
        actualizar(codVacacionAsignada, actual.getDiasAsignados(), actual.getMotivo(),
                   actual.getFecha(), audUsuarioI);

        ejecutar(SQL_BAJA, "dar de baja la vacación asignada", ps -> {
            ps.setLong(1, codVacacionAsignada);
            ps.setString(2, "D");
        });

        log.info("Baja de vacacion asignada {}: empleado={}, fecha={}, dias={}, usuario={}",
                 codVacacionAsignada, actual.getCodEmpleado(), actual.getFecha(),
                 actual.getDiasAsignados(), audUsuarioI);
        return actual;
    }

    /**
     * La {@code 'U'} pelada. El SP sólo pisa días, motivo, fecha y auditoría.
     *
     * <p>{@code ejecutarUpdate} y no {@code ejecutar}: {@code dau_vacacionAsignada} es un
     * <b>INSTEAD OF UPDATE</b> con {@code SET NOCOUNT ON}, así que el rowcount llega en 0 aunque la
     * fila se haya guardado. Ver {@link SpEscritura#ejecutarUpdate}.
     */
    private void actualizar(long codVacacionAsignada, double dias, String motivo, Date fecha,
                            long audUsuarioI) {
        SpEscritura.ejecutarUpdate(jdbcTemplate, SQL_EDICION, "editar la vacación asignada", ps -> {
            ps.setLong(1, codVacacionAsignada);
            ps.setDouble(2, dias);
            ps.setString(3, motivo);
            ps.setDate(4, new java.sql.Date(fecha.getTime()));
            ps.setLong(5, audUsuarioI);
            ps.setString(6, "U");
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Reglas — package-private y estáticas para poder probarlas sin base
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <b>Cero es válido</b> y no es un descuido: 446 de las 1.424 filas tienen cero días, que es
     * como se registra "este año no le corresponde nada". El legacy valida {@code >= 0} y dice
     * "No la cantidad de dia es menor a cero".
     *
     * @param confirmado si el usuario ya confirmó un valor fuera del rango histórico
     */
    static void validarDias(double dias, boolean confirmado) {
        if (dias < 0) {
            throw new SpBusinessException("Los días asignados no pueden ser negativos.");
        }
        if (dias > DIAS_TOPE) {
            throw new SpBusinessException(
                    "No se pueden asignar más de " + (int) DIAS_TOPE + " días en un solo registro.");
        }
        if (dias > DIAS_INUSUAL && !confirmado) {
            // 400 CONFIRMABLE y no 409: esto SÍ se resuelve reintentando —con la confirmación
            // puesta—, que es justo lo que SpConflictException documenta que no debe pasar.
            throw new SpConfirmableException(
                    "Va a asignar " + Fmt.dias(dias) + ", y lo habitual son hasta "
                  + Fmt.dias(DIAS_INUSUAL) + ". Confirme si es correcto.");
        }
    }

    /**
     * Obligatorio y de 2 a 300 caracteres — el largo real de la columna, que es NOT NULL.
     *
     * <p><b>No se copia la lista de caracteres del regex legacy</b>
     * ({@code ^[a-zA-Z0-9 óÓáÁéÉíÍúÚñÑ...]}): es un defecto, no una regla. Rechaza cosas que la
     * columna guarda sin problema y varía entre los dos formularios del ERP.
     */
    static String validarMotivo(String motivo) {
        String limpio = motivo == null ? "" : motivo.trim();
        if (limpio.length() < 2) {
            throw new SpBusinessException("El motivo de la vacación asignada es obligatorio.");
        }
        if (limpio.length() > MOTIVO_MAX) {
            throw new SpBusinessException(
                    "El motivo no puede pasar de " + MOTIVO_MAX + " caracteres (tiene "
                  + limpio.length() + ").");
        }
        return limpio;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Auxiliares
    // ══════════════════════════════════════════════════════════════════════

    /**
     * La fecha de un alta NO es libre: tiene que ser uno de los aniversarios que arma la ACCION
     * 'B' recorriendo de año en año desde {@code tb_relEmplEmpr.fechaIni} de
     * {@code codRelBeneficios}. En el legacy el campo va {@code disabled} y la fecha sale de la
     * fila sintética; acá se comprueba, porque un POST puede traer cualquier cosa.
     *
     * <h3>Duplicado, en dos niveles</h3>
     * <ul>
     *   <li><b>Doble toque</b> — ya hay una fila IDÉNTICA cargada hace menos de
     *       {@link #VENTANA_DOBLE_TOQUE_MS}: se rechaza con 409 <b>aunque venga confirmado</b>. El
     *       segundo toque del teléfono manda exactamente el mismo cuerpo, confirmación incluida,
     *       así que respetar el flag acá sería no proteger nada.</li>
     *   <li><b>Repetición deliberada</b> — ya hay una fila en ese aniversario, pero vieja o
     *       distinta: se pide confirmación (400) y se deja pasar. No se bloquea a ciegas porque el
     *       histórico tiene 215 grupos legítimamente repetidos por (empleado, relación, fecha),
     *       casi todos del proceso automático.</li>
     * </ul>
     */
    private void validarAniversarioLibre(long codEmpleado, long codRel, Date fecha,
                                         double dias, String motivo, boolean confirmado) {
        List<VacacionAsignada> grilla = historial(codEmpleado, codRel, fecha);
        if (grilla.isEmpty()) {
            throw new SpBusinessException(
                    "El empleado no tiene fecha de inicio de beneficio, así que no se le pueden "
                  + "generar aniversarios de vacación. Asígnele la relación de beneficios en el ERP.");
        }

        VacacionAsignada sintetica = null;
        VacacionAsignada real = null;
        for (VacacionAsignada f : grilla) {
            if (!SpEscritura.mismoDia(f.getFecha(), fecha)) continue;
            if (f.isSintetico()) sintetica = f; else real = f;
        }

        if (sintetica == null && real == null) {
            throw new SpBusinessException(
                    "La fecha indicada no es un aniversario de la relación laboral del empleado. "
                  + "El alta se hace sobre uno de los años que muestra el desglose.");
        }
        if (sintetica != null) return;   // el aniversario está libre, no hay nada más que mirar

        // La ACCION 'B' no devuelve la auditoría (su tabla temporal no la tiene), así que el
        // doble toque se detecta con la 'L', que sí trae audFechaI.
        long ahora = System.currentTimeMillis();
        for (VacacionAsignada e : filasEn(codEmpleado, codRel, fecha)) {
            boolean identica = Double.compare(e.getDiasAsignados(), dias) == 0
                    && motivo.equalsIgnoreCase(e.getMotivo() == null ? "" : e.getMotivo().trim());
            boolean reciente = e.getAudFechaI() != null
                    && ahora - e.getAudFechaI().getTime() < VENTANA_DOBLE_TOQUE_MS;
            if (identica && reciente) {
                throw new SpConflictException(
                        "Esa misma vacación asignada acaba de registrarse (registro "
                      + e.getCodVacacionAsignada() + "). No se cargó de nuevo.");
            }
        }
        if (!confirmado) {
            // 400 confirmable: repetición legítima. El cliente ofrece "Confirmar" y reintenta.
            throw new SpConfirmableException(
                    "Ese aniversario ya tiene una vacación asignada de " + Fmt.dias(real.getDiasAsignados())
                  + " (registro " + real.getCodVacacionAsignada() + "). Si de verdad hay que "
                  + "sumar otra, confirme; si no, edite la existente.");
        }
    }

    /**
     * La fila recién insertada. Como no hay id generado se busca por la misma terna que leen los
     * SP —(empleado, relación, fecha)— y se toma la de id más alto, que es la última en entrar.
     */
    private VacacionAsignada ultimaEn(long codEmpleado, long codRel, Date fecha) {
        VacacionAsignada ultima = null;
        for (VacacionAsignada f : filasEn(codEmpleado, codRel, fecha)) {
            if (ultima == null || f.getCodVacacionAsignada() > ultima.getCodVacacionAsignada()) {
                ultima = f;
            }
        }
        return ultima == null ? null : decorar(ultima);
    }

    /**
     * Las filas REALES de esa terna, con auditoría incluida — la ACCION 'L', que no tiene joins ni
     * inventa nada. Es la misma terna por la que filtran todas las lecturas del saldo.
     */
    private List<VacacionAsignada> filasEn(long codEmpleado, long codRel, Date fecha) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codEmpleado", codEmpleado);
        filtro.put("codRelEmplEmpr", codRel);
        filtro.put("fecha", new java.sql.Date(fecha.getTime()));
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", VacacionAsignada.class);
    }

    private VacacionAsignada decorar(VacacionAsignada v) {
        v.setSintetico(v.getCodVacacionAsignada() <= 0);
        v.setDiasAsignadosTxt(Fmt.dias(v.getDiasAsignados()));
        return v;
    }

    private void ejecutar(String sql, String queHacia, PreparedStatementSetter setter) {
        SpEscritura.ejecutar(jdbcTemplate, sql, queHacia, setter);
    }
}
