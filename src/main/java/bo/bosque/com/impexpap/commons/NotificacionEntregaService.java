package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.IEntregaChofer;
import bo.bosque.com.impexpap.dto.EntregaNotificacionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aviso de WhatsApp al CLIENTE en el momento en que su entrega queda marcada como
 * entregada desde el frontend. Es el único mensaje que manda este servicio.
 *
 * <h3>La regla que manda sobre todo lo demás</h3>
 * <b>Este servicio nunca puede romper ni demorar el registro de una entrega.</b> El chofer
 * está en la calle, con mala señal, y lo único que importa es que el
 * {@code POST /entregas/registro-entrega-chofer} devuelva 201. Por eso:
 * <ul>
 *   <li>{@link #notificarEntregaCompletada(long, String)} es {@code @Async}: corre en el
 *       pool {@code notificacionesExecutor} (ver {@code config.AsyncConfig}), nunca en el
 *       hilo del request.</li>
 *   <li>El método público atrapa {@code Throwable} — no {@code Exception} — y no relanza
 *       NADA. Un {@code NullPointerException} del DAO, un {@code NoClassDefFoundError},
 *       lo que sea: se loguea y se sigue.</li>
 *   <li>No hay valor de retorno ni {@code Future}: el llamador no tiene con qué esperar
 *       ni con qué fallar.</li>
 * </ul>
 *
 * <h3>Trampa conocida: la auto-invocación anula el {@code @Async}</h3>
 * {@code @Async} funciona por proxy. Si {@code notificarEntregaCompletada} se llamara
 * desde otro método de esta misma clase, o desde adentro del DAO sin pasar por el bean de
 * Spring, correría SÍNCRONA en el hilo del chofer — con el envío HTTP a openWA adentro.
 * Se debe inyectar este servicio y llamarlo desde afuera (el controlador de entregas),
 * después de confirmar que {@code registrarEntregaChofer} devolvió {@code true} y en su
 * propio {@code try/catch}.
 *
 * <h3>Estado actual</h3>
 * Apagado por defecto ({@code openwa.entregas.notificar-cliente=false}). El teléfono sale de
 * SAP vía {@link ContactoClienteSapResolver}; encenderlo hace que salgan mensajes hacia
 * AFUERA de la empresa, así que es una decisión deliberada y no un default.
 *
 * <h3>Pedido de calificación ({@code openwa.entregas.pedir-calificacion})</h3>
 * Con el flag encendido, al final del mismo mensaje —y solo de ese mensaje, no se manda uno
 * aparte— se le pide al cliente que responda con un número del 1 al 5, y el envío exitoso deja
 * una fila esperando esa respuesta vía {@link CalificacionService#registrarEnvio}. La respuesta
 * la levanta después el webhook de openWA; este servicio no la lee.
 *
 * <p>Dos consecuencias que conviene tener presentes:
 * <ul>
 *   <li>El pie del mensaje cambia. Sin calificación dice "por favor no responda a este chat";
 *       con calificación esa frase sería una contradicción, así que se reemplaza por una que
 *       invita a responder solo con el número.</li>
 *   <li>Encender el pedido sin el webhook configurado ({@code openwa.webhook.habilitado}) hace
 *       que se le pida al cliente algo que nadie va a leer, y deja filas pendientes para
 *       siempre. Por eso {@link #anunciarModo()} lo avisa en el arranque.</li>
 * </ul>
 */
@Service
public class NotificacionEntregaService {

    private static final Logger logger = LoggerFactory.getLogger(NotificacionEntregaService.class);

    /**
     * Firma que va al pie del mensaje al CLIENTE. Es lo único de la redacción que puede
     * querer cambiar quien administre el negocio; se deja acá para no tener que buscarlo
     * entre los StringBuilder.
     */
    private static final String FIRMA_EMPRESA = "Bosque";

    /**
     * Lo que se le pide al cliente cuando {@code openwa.entregas.pedir-calificacion=true}.
     *
     * <p>Está redactado con dos condiciones que no son de estilo sino funcionales:
     * <ul>
     *   <li><b>Se pide el número solo, y se dice el rango completo.</b> Del otro lado hay un
     *       parser ({@code CalificacionService.extraerPuntaje}), no una persona: cuanto más
     *       corta y más pautada sea la respuesta, menos respuestas se pierden.</li>
     *   <li><b>Se aclara qué extremo es cuál.</b> Sin eso, un "1" es ambiguo — hay clientes que
     *       lo mandan como "primer lugar" — y la calificación entera queda sin significado.</li>
     * </ul>
     *
     * <p>El tuteo/voseo se evita a propósito: el resto del mensaje trata al cliente de usted
     * ("su pedido", "comuníquese"), y mezclar los dos registros en el mismo texto se nota.
     */
    private static final String TEXTO_PEDIDO_CALIFICACION =
            "¿Cómo estuvo la entrega? Responda a este mensaje solo con un número del *1 al 5*, "
          + "donde 1 es la peor y 5 la mejor.";

    /**
     * Ventana de deduplicación de avisos al cliente, en milisegundos.
     *
     * <p><b>Por qué hace falta.</b> El SP {@code p_abm_trch_Entregas} ACCIÓN 'B' marca
     * {@code fueEntregado=1} en TODAS las filas del documento, y un {@code docEntry} tiene
     * una fila por {@code itemCode}. Si la app móvil llegara a mandar una petición por
     * ítem, el cliente recibiría N mensajes idénticos por la misma entrega. Cinco minutos
     * alcanzan para tapar esa ráfaga sin bloquear un reenvío legítimo más tarde.
     */
    private static final long VENTANA_DEDUPE_MS = 5L * 60L * 1000L;

    /** Tope duro de la caché de deduplicación, por si algún día llueven entregas. */
    private static final int MAX_CLAVES_DEDUPE = 2000;

    private final IEntregaChofer entregaChoferDao;
    private final WhatsAppService whatsAppService;
    private final ContactoClienteResolver contactoClienteResolver;
    private final CalificacionService calificacionService;

    /**
     * Aviso al cliente. APAGADO por defecto: encenderlo hace que salgan mensajes hacia
     * afuera de la empresa.
     */
    @Value("${openwa.entregas.notificar-cliente:false}")
    private boolean notificarCliente;

    /**
     * Tipos de documento que sí llevan aviso al cliente, separados por coma.
     *
     * <p>Es una lista blanca y no una lista negra a propósito: si mañana la sincronización con
     * SAP empieza a traer un tipo nuevo, lo seguro es NO avisarle a nadie hasta que alguien lo
     * agregue acá, en vez de mandar un WhatsApp a un destinatario que nadie previó.
     */
    @Value("${openwa.entregas.tipos-con-aviso:Factura,Orden Venta}")
    private String tiposConAviso;

    /**
     * Modo simulación: recorre TODO el camino real —lee el documento, filtra por tipo, resuelve
     * el teléfono contra SAP y arma el texto— pero en lugar de llamar a openWA escribe el
     * mensaje en el log. No sale ni un WhatsApp.
     *
     * <p><b>Por qué existe.</b> Sin esto no hay forma de probar: el flag
     * {@code notificar-cliente} se evalúa antes que nada, así que con el aviso apagado no se
     * ejecuta ni la consulta ni la resolución del teléfono, y con el aviso encendido ya salen
     * mensajes reales a clientes reales. No había punto intermedio. Dejándolo un día en
     * producción se ve exactamente a quién le habría llegado qué, cuántos se quedan sin
     * teléfono y si algún tipo de documento inesperado se cuela — todo antes de que un cliente
     * reciba el primer mensaje.
     *
     * <p>Gana sobre {@code notificar-cliente}: si los dos están en {@code true}, se simula.
     * Es deliberado — el modo que NO manda nada siempre tiene que poder frenar al que sí manda.
     */
    @Value("${openwa.entregas.simular:false}")
    private boolean simular;

    /**
     * Número de PRUEBA. Si está seteado, se manda de verdad por openWA pero <b>todos</b> los
     * avisos van a este número, sin importar de qué cliente sea la entrega.
     *
     * <p>Es el escalón entre {@link #simular} (no sale nada) y
     * {@link #notificarCliente} (sale a clientes reales): permite ver el mensaje llegando a un
     * WhatsApp de verdad —con su formato, sus emojis, sus saltos de línea— sin que ningún
     * cliente reciba nada.
     *
     * <p><b>No hace falta encender {@code notificar-cliente} para usarlo.</b> Alcanza con
     * poner un número acá. Es deliberado: nadie debería tener que activar el flag que manda a
     * clientes reales para hacer una prueba.
     */
    @Value("${openwa.entregas.telefono-prueba:}")
    private String telefonoPrueba;

    /**
     * Pedido de calificación al final del aviso. APAGADO por defecto.
     *
     * <p>Encenderlo cambia dos cosas a la vez, y por eso es un solo flag y no dos: el texto que
     * lee el cliente (se le pide un número y el pie deja de decir "no responda") y el registro de
     * la fila que va a esperar esa respuesta. Tenerlos separados permitiría el peor de los
     * estados: pedirle al cliente que califique sin nada que reciba la respuesta, o filas
     * pendientes de una pregunta que nunca se hizo.
     *
     * <p>No sirve solo: la respuesta entra por el webhook de openWA
     * ({@code openwa.webhook.habilitado}). Ver {@link #anunciarModo()}.
     */
    @Value("${openwa.entregas.pedir-calificacion:false}")
    private boolean pedirCalificacion;

    /**
     * Solo para el aviso de arranque. Este servicio no atiende el webhook —lo hace
     * {@code WhatsAppWebhookController}— pero es el único lugar donde se sabe que se está por
     * pedirle una calificación al cliente, así que es donde tiene sentido avisar que del otro
     * lado no hay nadie escuchando.
     */
    @Value("${openwa.webhook.habilitado:false}")
    private boolean webhookHabilitado;

    /** El número de prueba ya normalizado a chatId, o {@code null} si no hay modo prueba. */
    private String chatIdPrueba;

    /**
     * Claves {@code docEntry|db} ya avisadas, con el instante en que se avisaron.
     * {@code ConcurrentHashMap} porque el pool tiene varios hilos.
     */
    private final ConcurrentHashMap<String, Long> avisosRecientes = new ConcurrentHashMap<String, Long>();

    public NotificacionEntregaService(IEntregaChofer entregaChoferDao,
                                      WhatsAppService whatsAppService,
                                      ContactoClienteResolver contactoClienteResolver,
                                      CalificacionService calificacionService) {
        this.entregaChoferDao = entregaChoferDao;
        this.whatsAppService = whatsAppService;
        this.contactoClienteResolver = contactoClienteResolver;
        this.calificacionService = calificacionService;
    }

    /**
     * Deja en el log, al arrancar, si de este backend van a salir mensajes hacia clientes.
     * Es la clase de cosa que uno quiere poder responder mirando el arranque y no leyendo
     * tres archivos de configuración.
     */
    @javax.annotation.PostConstruct
    void anunciarModo() {
        if (telefonoPrueba != null && !telefonoPrueba.trim().isEmpty()) {
            chatIdPrueba = WhatsAppService.normalizarChatId(telefonoPrueba);
            if (chatIdPrueba == null) {
                logger.error("openwa.entregas.telefono-prueba='{}' no es un número válido; el modo prueba "
                           + "queda DESACTIVADO. Formato esperado: 8 dígitos empezando en 6 o 7.",
                             telefonoPrueba);
            }
        }

        if (simular) {
            logger.warn("Aviso al cliente en MODO SIMULACIÓN (openwa.entregas.simular=true): se recorre "
                      + "todo el camino y se loguea el mensaje, pero NO se envía nada a openWA.");
        } else if (chatIdPrueba != null) {
            logger.warn("Aviso al cliente en MODO PRUEBA: se envía de verdad por openWA, pero TODOS los "
                      + "avisos van a {} en lugar del cliente. Ningún cliente recibe nada.", chatIdPrueba);
        } else if (notificarCliente) {
            logger.warn("Aviso al cliente ACTIVO (openwa.entregas.notificar-cliente=true): a partir de ahora "
                      + "salen mensajes de WhatsApp a clientes reales. Tipos con aviso: {}", tiposConAviso);
        } else {
            logger.info("Aviso al cliente apagado. Para probar sin enviar: openwa.entregas.simular=true; "
                      + "para probar enviando a un solo número: openwa.entregas.telefono-prueba=7XXXXXXX");
        }

        anunciarCalificacion();
    }

    /**
     * Segunda mitad del aviso de arranque, dedicada a la calificación. Está separada de
     * {@link #anunciarModo()} porque el pedido de calificación es ortogonal a los tres modos de
     * envío: puede estar encendido con cualquiera de ellos.
     */
    private void anunciarCalificacion() {
        if (!pedirCalificacion) {
            logger.info("Pedido de calificación apagado (openwa.entregas.pedir-calificacion=false): el aviso "
                      + "termina con 'por favor no responda a este chat' y no se registra nada.");
            return;
        }

        if (!webhookHabilitado) {
            // Este es el estado malo de verdad, y es silencioso: el cliente responde "5", nadie
            // lo lee, y la fila queda pendiente para siempre. Peor todavía, el mensaje le pidió
            // algo y la empresa no contesta.
            logger.warn("Pedido de calificación ENCENDIDO pero el webhook está APAGADO "
                      + "(openwa.webhook.habilitado=false). Se le va a pedir al cliente que responda con un "
                      + "número y NADIE va a leer esa respuesta: las filas quedan pendientes para siempre. "
                      + "O se enciende openwa.webhook.habilitado, o se apaga openwa.entregas.pedir-calificacion.");
            return;
        }

        if (simular) {
            logger.warn("Pedido de calificación ENCENDIDO, pero como se está simulando no sale ningún mensaje "
                      + "y no se registra ninguna fila a la espera de respuesta.");
        } else if (chatIdPrueba != null) {
            logger.warn("Pedido de calificación ENCENDIDO en MODO PRUEBA: la fila queda esperando la respuesta "
                      + "de {} (el número de prueba), que es de donde va a llegar. Ningún cliente califica nada.",
                        chatIdPrueba);
        } else {
            logger.warn("Pedido de calificación ENCENDIDO: el aviso pide al cliente un número del 1 al 5 y cada "
                      + "envío exitoso deja una fila esperando la respuesta.");
        }
    }

    /**
     * Avisa al cliente que su entrega fue completada. Se llama una vez por entrega, o sea
     * por par ({@code docEntry}, {@code db}) — no por ítem.
     *
     * <p>Corre en el pool {@code notificacionesExecutor}. <b>Jamás lanza una excepción</b>:
     * cualquier {@code Throwable} muere en el {@code catch} de abajo. Si el pool está
     * saturado, la tarea ni siquiera llega a ejecutarse (se descarta con un WARN, ver
     * {@code AsyncConfig}); eso también es un final aceptable.
     *
     * <p>Secuencia: flag → deduplicación → datos del DAO → contacto → envío. El flag va
     * PRIMERO a propósito: mientras el aviso esté apagado no tiene sentido pegarle a la
     * base para armar un mensaje que no se va a mandar, y así tampoco se ensucia el log si
     * la ACCIÓN 'G' del SP todavía no está desplegada.
     *
     * @param docEntry documento de la entrega
     * @param db       base/empresa del documento, tal como vino en el request
     */
    @Async("notificacionesExecutor")
    public void notificarEntregaCompletada(long docEntry, String db) {
        try {
            // Tres modos, en orden de "no manda nada" a "manda a clientes reales":
            //   simular          -> loguea el mensaje, no envía
            //   telefono-prueba  -> envía de verdad, pero todo a un solo número
            //   notificar-cliente-> envía al cliente
            // Basta con cualquiera de los tres para entrar; la precedencia se resuelve abajo.
            if (!notificarCliente && !simular && chatIdPrueba == null) {
                logger.debug("Aviso al cliente deshabilitado (notificar-cliente=false, simular=false, "
                           + "sin telefono-prueba); se omite docEntry={} db={}", docEntry, db);
                return;
            }

            if (fueAvisadoRecientemente(docEntry, db)) {
                logger.debug("Aviso al cliente ya enviado hace menos de {} min para docEntry={} db={}; se omite.",
                             VENTANA_DEDUPE_MS / 60000L, docEntry, db);
                return;
            }

            EntregaNotificacionDTO dto = entregaChoferDao.obtenerDatosNotificacion(docEntry, db);
            if (dto == null) {
                // Incluye el caso "el documento tiene varios clientes": el DAO devuelve null
                // a propósito porque no hay forma de saber a cuál avisarle.
                logger.warn("No se pudieron obtener los datos de notificación de la entrega docEntry={} db={}; "
                          + "no se avisa al cliente.", docEntry, db);
                return;
            }

            // Solo se avisa por documentos que tienen un cliente del otro lado. Un 'Traspaso'
            // es un movimiento entre almacenes propios: no tiene número de nota ni cliente, y
            // su cardCode viene SIEMPRE vacío (2.344 de 2.344 filas en datos reales). Son
            // 1.108 documentos, o sea que sin este filtro uno de cada diez avisos intentaría
            // resolver el teléfono de un almacén.
            String tipoDoc = texto(dto.getTipo(), "");
            if (!tipoPermitido(tipoDoc)) {
                logger.info("La entrega docEntry={} db={} es de tipo '{}', que no lleva aviso al cliente "
                          + "(permitidos: {}). Se omite.",
                            docEntry, db, tipoDoc.isEmpty() ? "(vacío)" : tipoDoc, tiposConAviso);
                return;
            }

            // El cardCode y la base contra la que se busca el teléfono TIENEN que salir de la
            // MISMA fila. Usar el 'db' del request en vez del de la fila es un riesgo real, no
            // teórico: 1147 de los 1171 cardCodes con entregas existen en más de una base SAP,
            // 38 de ellos resuelven a dos celulares válidos DISTINTOS, y hay 370 docEntry que
            // aparecen bajo más de un 'db'. Un desalineamiento manda el aviso a otra persona.
            String dbFila = dto.getDb();
            if (dbFila == null || dbFila.trim().isEmpty()) {
                logger.warn("La fila de docEntry={} no trae 'db'; no se puede resolver el contacto con seguridad.",
                            docEntry);
                return;
            }
            if (db != null && !dbFila.trim().equalsIgnoreCase(db.trim())) {
                logger.warn("El 'db' del request ('{}') no coincide con el de la fila ('{}') para docEntry={}; "
                          + "se usa el de la fila.", db, dbFila, docEntry);
            }

            // Se resuelve el destinatario REAL siempre, incluso en modo prueba: es el dato que
            // dice si este cliente habría recibido el aviso, y es justamente lo que se quiere
            // medir durante las pruebas.
            String chatIdReal = contactoClienteResolver.resolverChatId(dto.getCardCode(), dbFila);

            String chatId;
            if (chatIdPrueba != null) {
                chatId = chatIdPrueba;
                logger.warn("[PRUEBA] Aviso REDIRIGIDO a {} — el destinatario real habría sido {} "
                          + "(cliente {} / {}), docEntry={} db={}.",
                            chatIdPrueba,
                            chatIdReal == null ? "NADIE: este cliente no tiene número usable" : chatIdReal,
                            dto.getCardCode(), texto(dto.getCardName(), "sin nombre"), docEntry, dbFila);
            } else if (chatIdReal == null || chatIdReal.trim().isEmpty()) {
                logger.info("Sin contacto de WhatsApp para el cliente {} ({}) — entrega docEntry={} db={} "
                          + "registrada, pero no se envía aviso.",
                            texto(dto.getCardName(), "sin nombre"), dto.getCardCode(), docEntry, dbFila);
                return;
            } else {
                chatId = chatIdReal;
            }

            String mensaje = armarMensajeCliente(dto);

            // En modo prueba el mensaje llega a un teléfono que NO es el del cliente. Sin este
            // encabezado, quien lo recibe lee un aviso dirigido a un tercero y no tiene forma
            // de saber a quién le habría llegado en producción — que es el dato que se está
            // yendo a verificar.
            if (chatIdPrueba != null) {
                mensaje = "🧪 *PRUEBA — este mensaje NO se envió al cliente*\n"
                        + "Destinatario real: " + texto(dto.getCardName(), "sin nombre")
                        + " (" + dto.getCardCode() + " / " + dbFila + ")\n"
                        + "Número real: "
                        + (chatIdReal == null ? "no tiene número usable" : chatIdReal) + "\n"
                        + "───────────────\n\n" + mensaje;
            }

            // Última barrera antes de que algo salga hacia afuera. Va acá abajo, y no arriba,
            // justamente para que la simulación ejerza TODO el camino: la consulta, el filtro
            // por tipo, la resolución del teléfono en SAP y el armado del texto. Lo único que
            // no ocurre es la llamada HTTP.
            if (simular) {
                logger.info("[SIMULACIÓN] NO se envía nada. docEntry={} db={} tipo={} cliente={} ({})"
                          + " destino={}\n--- mensaje que se habría enviado ---\n{}\n--- fin ---",
                            docEntry, dbFila, tipoDoc, dto.getCardCode(),
                            texto(dto.getCardName(), "sin nombre"), chatId, mensaje);
                marcarAvisado(docEntry, db);
                // No se registra la calificación: el cliente no recibió la pregunta, así que una
                // fila esperando su respuesta sería una fila pendiente para siempre. La simulación
                // no debe dejar rastro en la base, es su única promesa.
                return;
            }

            // La marca de dedupe se pone SOLO si openWA aceptó el mensaje. Si se marcara
            // antes, un openWA caído consumiría el único intento: el chofer reintenta el
            // POST un minuto después (mala señal en la calle es lo normal acá), openWA ya
            // se recuperó, y el cliente igual se quedaría sin aviso durante toda la ventana.
            if (whatsAppService.enviarMensajeACliente(chatId, mensaje)) {
                marcarAvisado(docEntry, db);
                logger.info("Aviso de entrega enviado al cliente {} (docEntry={} db={}).",
                            dto.getCardCode(), docEntry, dbFila);
                // Recién acá: la fila que espera la calificación se abre SOLO si la pregunta
                // efectivamente salió. Registrarla antes del envío dejaría pendientes todas las
                // entregas de un openWA caído, y el reporte por chofer contaría como "enviada"
                // una encuesta que el cliente nunca vio.
                registrarEnvioCalificacion(dto, chatId, docEntry, dbFila);
            } else {
                logger.warn("openWA no aceptó el aviso de la entrega docEntry={} db={} (cliente {}); "
                          + "no se marca como avisada, para que el reintento pueda volver a enviarlo.",
                            docEntry, dbFila, dto.getCardCode());
            }

        } catch (Throwable t) {
            // Techo absoluto: la entrega ya está registrada y devuelta al chofer. Acá no
            // se relanza nada bajo ninguna circunstancia.
            logger.error("Error al notificar la entrega docEntry={} db={}: {}", docEntry, db, t.getMessage(), t);
        }
    }

    // =====================================================================
    // Calificación
    // =====================================================================

    /**
     * Deja la fila que va a esperar la calificación del cliente. Se llama una sola vez por aviso,
     * y solo después de que openWA aceptó el mensaje.
     *
     * <p><b>El {@code chatId} que se guarda es el del destinatario REAL del mensaje, no el del
     * cliente.</b> En modo prueba los dos difieren, y guardar el del cliente rompería el circuito
     * completo: la respuesta va a llegar desde el número de prueba, y el webhook busca la fila
     * pendiente justamente por el chat desde el que le hablan. Es el mismo motivo por el que en
     * modo simulación no se registra nada — ahí no hay ningún chat del cual pueda venir una
     * respuesta.
     *
     * <p><b>Por qué tiene su propio {@code catch} teniendo el del método público arriba.</b> Sin
     * él, un fallo de la base al insertar la fila se loguearía como "Error al notificar la
     * entrega", que es falso: el WhatsApp ya salió y el cliente ya lo leyó. Lo único que se perdió
     * es la encuesta. El {@code catch} de arriba sigue estando como techo; este solo evita que un
     * problema menor se reporte como el problema grave.
     */
    private void registrarEnvioCalificacion(EntregaNotificacionDTO dto, String chatId,
                                            long docEntry, String dbFila) {
        if (!pedirCalificacion) {
            // El mensaje que salió no pidió ninguna calificación (armarMensajeCliente mira el
            // mismo flag), así que abrir la fila dejaría una espera que nadie provocó.
            return;
        }
        try {
            calificacionService.registrarEnvio(dto, chatId);
        } catch (Throwable t) {
            logger.error("El aviso de la entrega docEntry={} db={} SÍ se envió a {}, pero no se pudo registrar "
                       + "la calificación pendiente: {}. Si el cliente responde con un número, no va a haber "
                       + "fila donde guardarlo.", docEntry, dbFila, chatId, t.getMessage(), t);
        }
    }

    // =====================================================================
    // Deduplicación
    // =====================================================================

    /**
     * ¿Esta entrega ya se avisó dentro de la ventana de dedupe?
     *
     * <p><b>Consulta pura: no marca nada.</b> La marca la pone {@link #marcarAvisado} recién
     * cuando openWA confirma el envío. Separar las dos mitades es el punto: mientras estaban
     * juntas, un fallo de envío igual consumía el intento y el reintento quedaba bloqueado.
     */
    private boolean fueAvisadoRecientemente(long docEntry, String db) {
        long ahora = System.currentTimeMillis();
        limpiarAvisosVencidos(ahora);
        // limpiarAvisosVencidos ya sacó todo lo que expiró, así que la sola presencia alcanza.
        return avisosRecientes.get(claveDedupe(docEntry, db)) != null;
    }

    /**
     * ¿A este tipo de documento le corresponde avisarle al cliente?
     *
     * <p>Compara sin distinguir mayúsculas ni espacios de sobra, porque el valor viene de la
     * sincronización con SAP y no de una tabla de catálogo: en los datos conviven
     * {@code 'Factura'}, {@code 'Orden Venta'} y {@code 'Traspaso'}, pero nada garantiza la
     * capitalización a futuro.
     */
    private boolean tipoPermitido(String tipo) {
        if (tipo == null || tipo.trim().isEmpty()) {
            return false;
        }
        String buscado = tipo.trim().toLowerCase(Locale.ROOT);
        String[] permitidos = (tiposConAviso == null ? "" : tiposConAviso).split(",");
        for (int i = 0; i < permitidos.length; i++) {
            if (buscado.equals(permitidos[i].trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** Deja constancia de que el aviso salió, para no repetirlo si llega un reintento. */
    private void marcarAvisado(long docEntry, String db) {
        avisosRecientes.put(claveDedupe(docEntry, db), Long.valueOf(System.currentTimeMillis()));
    }

    /** La entrega es el par (docEntry, db); {@code db} se normaliza porque llega del cliente. */
    private String claveDedupe(long docEntry, String db) {
        return docEntry + "|" + (db == null ? "" : db.trim().toUpperCase(Locale.ROOT));
    }

    /** Saca de la caché lo que ya venció. Es un mapa chico; recorrerlo entero no duele. */
    private void limpiarAvisosVencidos(long ahora) {
        if (avisosRecientes.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, Long>> it = avisosRecientes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entrada = it.next();
            if (ahora - entrada.getValue().longValue() > VENTANA_DEDUPE_MS) {
                it.remove();
            }
        }
        // Válvula de escape: si algo patológico llenó el mapa, se vacía entero. Perder la
        // memoria de dedupe solo puede causar un mensaje repetido; una fuga de memoria en
        // el backend es peor.
        if (avisosRecientes.size() > MAX_CLAVES_DEDUPE) {
            logger.warn("La caché de deduplicación superó {} claves; se vacía por completo.",
                        Integer.valueOf(MAX_CLAVES_DEDUPE));
            avisosRecientes.clear();
        }
    }

    // =====================================================================
    // Redacción del mensaje
    // =====================================================================

    /**
     * Arma el texto que lee el cliente. Formato WhatsApp: {@code *negrita*} y
     * {@code _cursiva_}.
     *
     * <p>Con {@code openwa.entregas.pedir-calificacion=true} se le agrega al final el pedido de
     * calificación y <b>cambia el pie</b>. Los dos van juntos y no por separado: el pie original
     * dice literalmente "por favor no responda a este chat", y dejarlo debajo de una pregunta
     * sería pedirle al cliente que responda y decirle en la línea siguiente que no lo haga. Con
     * el flag apagado el texto sale exactamente igual que antes, byte por byte.
     */
    private String armarMensajeCliente(EntregaNotificacionDTO dto) {
        StringBuilder sb = new StringBuilder();

        String cliente = sanearParaMensaje(texto(dto.getCardName(), null), 80);
        if (cliente != null) {
            sb.append("Hola *").append(cliente).append("*, ");
        } else {
            sb.append("Hola, ");
        }
        sb.append("le confirmamos que su pedido ya fue *entregado*.\n\n");

        if (dto.getDocNum() != null && dto.getDocNum().intValue() > 0) {
            sb.append("• Nota de entrega: *").append(dto.getDocNum()).append("*\n");
        }
        if (dto.getFactura() != null && dto.getFactura().intValue() > 0) {
            sb.append("• Factura: *").append(dto.getFactura()).append("*\n");
        }
        String fecha = texto(dto.getFechaEntrega(), null);
        if (fecha != null) {
            sb.append("• Fecha y hora: ").append(fecha).append("\n");
        }
        String direccion = sanearParaMensaje(texto(dto.getDireccionEntrega(), null), 120);
        if (direccion != null) {
            sb.append("• Dirección: ").append(direccion).append("\n");
        }
        if (dto.getCantidadItems() != null && dto.getCantidadItems().intValue() > 0) {
            sb.append("• Ítems entregados: ").append(dto.getCantidadItems()).append("\n");
        }

        sb.append("\nSi encuentra alguna diferencia con su pedido, comuníquese con su asesor de ventas.\n");
        sb.append("¡Gracias por su preferencia! 🙌\n");

        if (pedirCalificacion) {
            // El pedido va DESPUÉS del agradecimiento y no antes: partir "Gracias por su
            // preferencia" del cierre deja el mensaje sonando cortado. Y va en su propio
            // párrafo, separado, porque es lo único que espera una acción del cliente.
            sb.append("\n").append(TEXTO_PEDIDO_CALIFICACION).append("\n");
            sb.append("\n_").append(FIRMA_EMPRESA)
              .append(" — mensaje automático. Para calificar, responda solo con el número; "
                    + "para cualquier otra consulta, comuníquese con su asesor._");
        } else {
            sb.append("\n_").append(FIRMA_EMPRESA)
              .append(" — mensaje automático, por favor no responda a este chat._");
        }
        return sb.toString();
    }

    /**
     * Deja un texto de la base en condiciones de viajar dentro de un mensaje que lee el
     * CLIENTE y va firmado por la empresa.
     *
     * <p><b>Por qué esto existe.</b> {@code direccionEntrega} y {@code cardName} no son datos
     * de confianza: entran por el body de {@code POST /entregas/registro-entrega-chofer} y de
     * {@code /registro-inicio-fin-entrega}, y el SP los guarda tal cual
     * ({@code SET direccionEntrega = @direccionEntrega}). {@code direccionEntrega} es
     * {@code varchar(500)}: sin filtro, son 500 caracteres a elección de quien llama el
     * endpoint, entregados al teléfono real de un cliente y firmados por Bosque. Con el
     * markdown de WhatsApp y una URL, eso es un canal de phishing con la identidad de la
     * empresa.
     *
     * <p>Se hace lo mínimo que corta el abuso sin arruinar direcciones legítimas:
     * <ul>
     *   <li>se colapsan los saltos de línea y los espacios repetidos (nadie inyecta una
     *       "firma" falsa abajo del mensaje);</li>
     *   <li>se quitan los caracteres de formato de WhatsApp ({@code * _ ~ `});</li>
     *   <li>si aparece algo que huela a enlace, el campo entero se descarta — una dirección
     *       de entrega no necesita una URL;</li>
     *   <li>se recorta a un largo razonable.</li>
     * </ul>
     *
     * <p>Esto es una contención, no la solución de fondo: lo que corresponde es que el
     * endpoint valide su entrada y que la dirección salga de {@code addressEntregaFac} /
     * {@code addressEntregaMat}, que vienen de SAP y no del request.
     *
     * @return el texto saneado, o {@code null} si quedó vacío o si se descartó
     */
    static String sanearParaMensaje(String valor, int largoMaximo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replaceAll("\\s+", " ").trim();
        if (limpio.isEmpty()) {
            return null;
        }

        String enMinusculas = limpio.toLowerCase(Locale.ROOT);
        if (enMinusculas.contains("http") || enMinusculas.contains("://")
                || enMinusculas.contains("www.") || enMinusculas.contains("wa.me")) {
            logger.warn("Se descarta un campo del mensaje al cliente por contener algo parecido a un enlace: '{}'",
                        limpio);
            return null;
        }

        limpio = limpio.replaceAll("[*_~`\\[\\]]", "").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > largoMaximo) {
            limpio = limpio.substring(0, largoMaximo).trim() + "…";
        }
        return limpio;
    }

    /** Devuelve el texto sin espacios sobrantes, o {@code porDefecto} si viene nulo o vacío. */
    private String texto(String valor, String porDefecto) {
        if (valor == null) {
            return porDefecto;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? porDefecto : limpio;
    }
}
