package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.ICalificacionEntrega;
import bo.bosque.com.impexpap.dto.EntregaNotificacionDTO;
import bo.bosque.com.impexpap.model.CalificacionEntrega;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encuesta de calificación de la entrega: deja la fila esperando respuesta cuando sale el aviso
 * al cliente, e interpreta lo que el cliente contesta por WhatsApp.
 *
 * <h3>Las dos mitades del feature</h3>
 * <ol>
 *   <li>{@link #registrarEnvio(EntregaNotificacionDTO, String)} — la llama
 *       {@code NotificacionEntregaService} justo después de que openWA aceptó el aviso de
 *       entrega. Inserta la fila con {@code puntaje = NULL}: eso es lo que la vuelve
 *       "pendiente".</li>
 *   <li>{@link #procesarRespuesta(String, String)} — la llama
 *       {@code controller.WhatsAppWebhookController} cuando entra un {@code message.received}.
 *       Si el texto contiene un puntaje reconocible, busca la fila pendiente de ese chat y la
 *       completa.</li>
 * </ol>
 *
 * <h3>La regla que manda sobre todo lo demás (heredada de NotificacionEntregaService)</h3>
 * <b>Nada de acá puede romper ni demorar el registro de una entrega, ni tumbar el webhook.</b>
 * Los dos métodos públicos atrapan {@code Throwable} y no relanzan NADA:
 * {@link #registrarEnvio} cuelga del hilo asíncrono del aviso al chofer, y
 * {@link #procesarRespuesta} cuelga de un endpoint público al que le escribe cualquiera. El DAO
 * ya promete no lanzar (ver {@link ICalificacionEntrega}); el {@code catch} de acá es la segunda
 * red, por si el bean viene null o el SP todavía no está desplegado.
 *
 * <h3>Qué NO hace este servicio</h3>
 * No manda el pedido de calificación: ese texto va pegado al aviso de entrega que arma
 * {@code NotificacionEntregaService.armarMensajeCliente(...)}. Acá solo se registra la fila
 * pendiente y se interpreta la respuesta. La razón es que el cliente tiene que recibir UN solo
 * mensaje, no dos.
 *
 * <h3>Estado actual</h3>
 * Apagado por defecto ({@code openwa.entregas.pedir-calificacion=false}). Encenderlo cambia el
 * texto que reciben clientes reales y empieza a escribir filas en
 * {@code trch_EntregaCalificacion}, así que es una decisión deliberada y no un default.
 *
 * @see #extraerPuntaje(String) el corazón del feature: qué se considera una calificación válida
 */
@Service
public class CalificacionService {

    private static final Logger logger = LoggerFactory.getLogger(CalificacionService.class);

    // =====================================================================
    // Contrato con el SP p_abm_trch_EntregaCalificacion
    // =====================================================================

    /**
     * ACCIÓN que da de ALTA la encuesta pendiente (puntaje NULL).
     * Tiene que coincidir EXACTO con el {@code IF(@ACCION = 'I')} del SP; el DAO no valida la
     * letra, se la pasa tal cual y una letra desconocida no afecta ninguna fila y devuelve false.
     */
    static final String ACCION_INSERTAR_ENVIO = "I";

    /**
     * ACCIÓN que ACTUALIZA la fila pendiente con la respuesta del cliente. El SP ubica la fila
     * por {@code idCalificacion}, así que solo sirve sobre un objeto que vino de
     * {@link ICalificacionEntrega#buscarPendientePorChat(String, int)}.
     */
    static final String ACCION_GUARDAR_PUNTAJE = "U";

    // =====================================================================
    // Escala y límites
    // =====================================================================

    /** Puntaje mínimo válido. Un 0 NO es una calificación: es "no entendí lo que escribió". */
    private static final int PUNTAJE_MIN = 1;

    /** Puntaje máximo válido. La encuesta es de 1 a 5 estrellas. */
    private static final int PUNTAJE_MAX = 5;

    /** Ventana por defecto si la propiedad viene con un valor imposible. */
    private static final int VENTANA_HORAS_POR_DEFECTO = 48;

    /**
     * Largo máximo del mensaje para que un NÚMERO (dígito o palabra) cuente como calificación.
     *
     * <p><b>Por qué existe.</b> Sin este techo, cualquier mensaje con un número adentro se
     * convierte en un puntaje: {@code "gracias por todo, llegó a las 3 de la tarde"} guardaría un
     * 3, y {@code "el chofer esperó 2 horas"} guardaría un 2. En un texto largo el número casi
     * nunca es la nota: es una hora, una cantidad, un piso, un precio.
     *
     * <p>Las respuestas reales a "¿del 1 al 5?" son cortas: {@code "5"}, {@code "un 4"},
     * {@code "4 estrellas"}, {@code "muy buen servicio, un 5"} (23 caracteres). Treinta alcanza
     * para todas y deja afuera la prosa. El precio es perder alguna calificación legítima
     * enterrada en un párrafo — y eso está bien: se prefiere perder una antes que inventar una,
     * porque un puntaje equivocado ensucia el promedio de un chofer y nadie se entera nunca.
     */
    private static final int MAX_LARGO_CON_NUMERO = 30;

    /**
     * Largo máximo del mensaje cuando la calificación viene en ESTRELLAS.
     *
     * <p>Las estrellas no sufren el problema de arriba: nadie escribe {@code "⭐⭐⭐⭐⭐"} en el
     * medio de una frase salvo para calificar. Por eso acá el techo es amplio y
     * {@code "muchas gracias, todo excelente y muy rápido ⭐⭐⭐⭐⭐"} se entiende igual. El límite
     * sigue existiendo solo para no barrer un mensaje enorme buscando estrellas.
     */
    private static final int MAX_LARGO_CON_ESTRELLAS = 200;

    /** Largo al que se recorta el comentario que se guarda. La columna tiene que aguantarlo. */
    private static final int MAX_LARGO_COMENTARIO = 250;

    /** Largo al que se recorta el mensaje crudo que se guarda para auditoría. */
    private static final int MAX_LARGO_MENSAJE_CRUDO = 500;

    /**
     * Largo al que se recortan el nombre del cliente y el del chofer antes de congelarlos en la
     * fila. {@code cardName} viene del request del chofer y no de un catálogo, así que puede venir
     * de cualquier largo; recortarlo acá evita que el SP falle por truncamiento.
     */
    private static final int MAX_LARGO_NOMBRE = 100;

    /** Formato en el que la ACCIÓN 'G' del SP de entregas devuelve la fecha, ya como texto. */
    private static final String FORMATO_FECHA_ENTREGA = "dd/MM/yyyy HH:mm";

    /** Zona del servidor. La misma que usa {@code config.JacksonConfig}. */
    private static final String ZONA_HORARIA = "America/La_Paz";

    // =====================================================================
    // Reconocimiento del puntaje
    // =====================================================================

    /**
     * Estrellas LLENAS que se cuentan como voto: ⭐ (U+2B50), ★ (U+2605) y 🌟 (U+1F31F).
     *
     * <p>Las estrellas VACÍAS (☆ U+2606) quedan afuera a propósito: quien manda
     * {@code "⭐⭐⭐☆☆"} está diciendo 3, no 5. Contando solo las llenas, ese caso sale bien sin
     * ninguna regla extra.
     */
    private static final int ESTRELLA_LLENA_BLANCA = 0x2B50;
    private static final int ESTRELLA_LLENA_NEGRA = 0x2605;
    private static final int ESTRELLA_BRILLANTE = 0x1F31F;

    /**
     * Respuestas del tipo "4/5", "5 de 5", "4 sobre 5".
     *
     * <p>Tiene que evaluarse ANTES que el barrido genérico de dígitos, porque para ese barrido
     * {@code "4/5"} son dos números distintos y la regla de "no adivinar" lo descartaría. Acá el
     * segundo 5 no es un candidato: es el denominador de la escala.
     *
     * <p>El patrón exige coincidencia COMPLETA ({@code matches()}) con {@code \D*} a los lados,
     * o sea que ningún otro número puede aparecer en el texto. Eso es lo que impide que una fecha
     * como {@code "5/5/2026"} entre por acá.
     */
    private static final Pattern PATRON_FRACCION =
            Pattern.compile("^\\D*([1-5])\\s*(?:/|de|sobre)\\s*5\\D*$");

    /**
     * Números escritos con letra. El valor puede estar FUERA de la escala a propósito: si el
     * cliente escribe "cero" o "siete", eso no es un puntaje válido y hay que descartar el
     * mensaje entero, no ignorar la palabra.
     *
     * <p><b>"un" y "una" NO están en el mapa, y es deliberado.</b> Casi nunca son el número:
     * {@code "un 4"} y {@code "una nota de 5"} son artículos. Mapearlos a 1 haría que
     * {@code "un 4"} tuviera dos candidatos (1 y 4) y terminara descartado — justo uno de los
     * casos que el feature tiene que acertar. El precio es que {@code "una estrella"} no se
     * entiende y se pierde; se prefiere eso a arruinar el caso frecuente.
     */
    private static final Map<Pattern, Integer> PALABRAS_NUMERO;

    static {
        Map<String, Integer> palabras = new LinkedHashMap<String, Integer>();
        palabras.put("cero", Integer.valueOf(0));
        palabras.put("uno", Integer.valueOf(1));
        palabras.put("dos", Integer.valueOf(2));
        palabras.put("tres", Integer.valueOf(3));
        palabras.put("cuatro", Integer.valueOf(4));
        palabras.put("cinco", Integer.valueOf(5));
        palabras.put("seis", Integer.valueOf(6));
        palabras.put("siete", Integer.valueOf(7));
        palabras.put("ocho", Integer.valueOf(8));
        palabras.put("nueve", Integer.valueOf(9));
        palabras.put("diez", Integer.valueOf(10));

        Map<Pattern, Integer> compiladas = new LinkedHashMap<Pattern, Integer>();
        for (Map.Entry<String, Integer> entrada : palabras.entrySet()) {
            // \b para que "veinticinco" no cuente como "cinco" ni "dosis" como "dos".
            compiladas.put(Pattern.compile("\\b" + entrada.getKey() + "\\b"), entrada.getValue());
        }
        PALABRAS_NUMERO = compiladas;
    }

    // =====================================================================
    // Colaboradores y configuración
    // =====================================================================

    private final ICalificacionEntrega calificacionDao;
    private final WhatsAppService whatsAppService;

    /**
     * Interruptor del feature. APAGADO por defecto: encenderlo cambia el texto que reciben
     * clientes reales y empieza a escribir filas en {@code trch_EntregaCalificacion}.
     *
     * <p>Se lee también acá, y no solo en {@code NotificacionEntregaService}, para que ninguna
     * fila pendiente nazca por un llamado que se olvidó de consultar el flag.
     */
    @Value("${openwa.entregas.pedir-calificacion:false}")
    private boolean pedirCalificacion;

    /**
     * Antigüedad máxima, en horas, de la entrega que un mensaje entrante puede calificar.
     *
     * <p>Es lo que evita que un "5" escrito tres semanas después, por cualquier otro motivo,
     * termine calificando una entrega vieja. 48 h cubre el fin de semana.
     */
    @Value("${openwa.entregas.calificacion-ventana-horas:48}")
    private int ventanaHoras;

    /**
     * Mismo flag de simulación que respeta {@code NotificacionEntregaService}: si está en
     * {@code true} no sale NADA hacia openWA. El acuse de recibo se loguea en vez de enviarse.
     */
    @Value("${openwa.entregas.simular:false}")
    private boolean simular;

    /**
     * Acuse de recibo al cliente después de guardar el puntaje ("gracias por calificarnos").
     *
     * <p>Encendido por defecto, pero solo puede dispararse cuando ya se guardó una calificación
     * real, o sea cuando el feature entero está encendido. Tiene default en el {@code @Value} a
     * propósito: no hace falta declarar la propiedad en {@code application.properties} para que
     * el arranque funcione.
     */
    @Value("${openwa.entregas.calificacion-acuse:true}")
    private boolean enviarAcuse;

    /**
     * Interruptor maestro de salida hacia clientes, el mismo que mira
     * {@code NotificacionEntregaService}.
     *
     * <p>Está acá porque el acuse de recibo es un mensaje que sale de la empresa hacia afuera
     * igual que el aviso de entrega, y sin esta guarda se escapaba por un costado: alcanzaba
     * con que llegara una respuesta al webhook para que el bot le contestara a un cliente real
     * aunque {@code notificar-cliente} estuviera en false. Todo camino que termine en un
     * WhatsApp a un cliente tiene que pasar por el mismo interruptor; si hay dos puertas, una
     * queda abierta.
     */
    @Value("${openwa.entregas.notificar-cliente:false}")
    private boolean notificarCliente;

    public CalificacionService(ICalificacionEntrega calificacionDao,
                               WhatsAppService whatsAppService) {
        this.calificacionDao = calificacionDao;
        this.whatsAppService = whatsAppService;
    }

    /**
     * Deja en el log, al arrancar, si la encuesta está encendida y con qué ventana. Mismo patrón
     * que {@code NotificacionEntregaService.anunciarModo()}: es la clase de cosa que uno quiere
     * poder responder mirando el arranque y no leyendo tres archivos de configuración.
     */
    @PostConstruct
    void anunciarModo() {
        if (ventanaHoras <= 0) {
            logger.warn("openwa.entregas.calificacion-ventana-horas={} no es un valor usable; "
                      + "se usa el default de {} horas.",
                        Integer.valueOf(ventanaHoras), Integer.valueOf(VENTANA_HORAS_POR_DEFECTO));
            ventanaHoras = VENTANA_HORAS_POR_DEFECTO;
        }
        if (pedirCalificacion) {
            logger.warn("Encuesta de calificación ACTIVA (openwa.entregas.pedir-calificacion=true): "
                      + "cada aviso de entrega deja una fila pendiente y se le pide al cliente que "
                      + "responda. Ventana de respuesta: {} h. Acuse de recibo: {}. Simulación: {}.",
                        Integer.valueOf(ventanaHoras), Boolean.valueOf(enviarAcuse), Boolean.valueOf(simular));
        } else {
            logger.info("Encuesta de calificación apagada (openwa.entregas.pedir-calificacion=false). "
                      + "No se registran envíos; un mensaje entrante nunca va a encontrar una fila "
                      + "pendiente que completar.");
        }
    }

    // =====================================================================
    // 1) Envío: dejar la fila esperando respuesta
    // =====================================================================

    /**
     * Registra que a este cliente se le pidió que califique la entrega. Deja la fila pendiente:
     * con {@code chatId} cargado y {@code puntaje} en NULL.
     *
     * <p><b>El {@code chatId} que se guarda tiene que ser EL MISMO al que se envió el mensaje.</b>
     * Es la única clave por la que el webhook puede después encontrar esta fila. En modo prueba
     * ({@code openwa.entregas.telefono-prueba}) el aviso se redirige al teléfono del tester, así
     * que lo que hay que pasar acá es el chatId de PRUEBA y no el del cliente; si se guardara el
     * del cliente, el "5" que responde el tester no encontraría ninguna encuesta y el feature
     * parecería roto sin serlo.
     *
     * <p><b>Nunca lanza.</b> Corre colgado del aviso de entrega, que a su vez cuelga del registro
     * que hace el chofer desde la calle.
     *
     * @param dto    datos de la entrega, tal como los devolvió la ACCIÓN 'G'
     * @param chatId chat al que se le mandó el pedido, ya normalizado ({@code 591XXXXXXXX@c.us})
     */
    public void registrarEnvio(EntregaNotificacionDTO dto, String chatId) {
        try {
            if (!pedirCalificacion) {
                logger.debug("Encuesta apagada (pedir-calificacion=false); no se registra el envío "
                           + "para docEntry={}.", dto == null ? null : dto.getDocEntry());
                return;
            }
            if (dto == null) {
                logger.warn("Se pidió registrar el envío de una encuesta sin datos de la entrega; se omite.");
                return;
            }
            String chat = WhatsAppService.normalizarChatId(chatId);
            if (chat == null) {
                // Sin chat no hay clave por la que reconocer la respuesta: la fila sería basura.
                logger.info("La entrega docEntry={} db={} no tiene un chat de WhatsApp usable ('{}'); "
                          + "no se registra la encuesta.", dto.getDocEntry(), dto.getDb(), chatId);
                return;
            }

            CalificacionEntrega mb = new CalificacionEntrega();
            mb.setDocEntry(dto.getDocEntry());
            mb.setDb(dto.getDb());
            mb.setCardCode(dto.getCardCode());
            mb.setCardName(recortar(dto.getCardName(), MAX_LARGO_NOMBRE));
            mb.setUChofer(dto.getUChofer());
            mb.setNombreChofer(recortar(dto.getNombreChofer(), MAX_LARGO_NOMBRE));
            mb.setChatId(chat);
            mb.setFechaEntrega(parsearFechaEntrega(dto.getFechaEntrega()));
            // puntaje, comentario, mensajeCrudo y fechaRespuesta quedan en NULL: eso es lo que
            // la ACCIÓN 'P' del SP de lectura reconoce como "pendiente".

            if (calificacionDao.registrarCalificacion(mb, ACCION_INSERTAR_ENVIO)) {
                logger.info("Encuesta de calificación registrada para docEntry={} db={} cliente={} chat={}.",
                            dto.getDocEntry(), dto.getDb(), dto.getCardCode(), chat);
            } else {
                logger.warn("No se pudo registrar la encuesta de calificación de docEntry={} db={} "
                          + "(chat={}). El aviso al cliente ya salió, así que si contesta no va a "
                          + "haber fila pendiente que completar.",
                            dto.getDocEntry(), dto.getDb(), chat);
            }
        } catch (Throwable t) {
            // Techo absoluto: la entrega ya está registrada y el aviso ya salió. Acá no se
            // relanza nada bajo ninguna circunstancia.
            logger.error("Error al registrar la encuesta de calificación (docEntry={}): {}",
                         dto == null ? null : dto.getDocEntry(), t.getMessage(), t);
        }
    }

    // =====================================================================
    // 2) Respuesta: interpretar lo que contestó el cliente
    // =====================================================================

    /**
     * Interpreta un mensaje entrante y, si trae un puntaje, lo guarda sobre la encuesta pendiente
     * de ese chat.
     *
     * <p>Orden de los pasos, y por qué ese orden: <b>primero se extrae el puntaje y recién
     * después se toca la base</b>. La enorme mayoría de los mensajes que entran por el webhook no
     * son calificaciones (son consultas, audios, "hola", stickers); si se consultara primero la
     * fila pendiente, cada mensaje del día pegaría contra el SP para nada.
     *
     * <p><b>Nunca lanza.</b> La llama un endpoint público.
     *
     * @param chatId chat del que llegó el mensaje, en cualquier formato ({@code 591X...@c.us})
     * @param texto  texto crudo del mensaje, tal como lo escribió el cliente
     * @return {@code true} solo si se guardó un puntaje
     */
    public boolean procesarRespuesta(String chatId, String texto) {
        try {
            // Si nunca se pidió una calificación, no hay nada que interpretar. Sin esta guarda
            // el webhook seguiría leyendo mensajes entrantes y buscando encuestas pendientes
            // con la función apagada — trabajo inútil contra la base, y peor: cualquier "3" que
            // un cliente le escriba al número por otro motivo quedaría registrado como puntaje.
            if (!pedirCalificacion) {
                logger.debug("Llegó un mensaje pero la calificación está apagada "
                           + "(openwa.entregas.pedir-calificacion=false); no se procesa.");
                return false;
            }

            String chat = WhatsAppService.normalizarChatId(chatId);
            if (chat == null) {
                // Incluye los ids de GRUPO: normalizarChatId los rechaza a propósito. Una
                // encuesta es siempre un chat individual.
                logger.debug("Mensaje entrante desde un chat que no es un contacto individual usable "
                           + "('{}'); no se procesa como calificación.", chatId);
                return false;
            }

            Integer puntaje = extraerPuntaje(texto);
            if (puntaje == null) {
                // Camino normal, no es un error: la mayoría de los mensajes no son puntajes.
                logger.debug("El mensaje de {} no contiene un puntaje reconocible; no se toca la base.", chat);
                return false;
            }

            CalificacionEntrega pendiente = calificacionDao.buscarPendientePorChat(chat, ventanaHoras);
            if (pendiente == null) {
                logger.info("{} respondió algo que parece un puntaje ({}), pero no tiene ninguna encuesta "
                          + "pendiente de las últimas {} h; no se guarda nada.",
                            chat, puntaje, Integer.valueOf(ventanaHoras));
                return false;
            }

            pendiente.setPuntaje(puntaje);
            // Texto libre del cliente: dato NO confiable. Se guarda por parámetro enlazado (lo
            // hace el DAO) y recortado. Si algún día este comentario se muestra dentro de un
            // mensaje de WhatsApp, TIENE que pasar antes por
            // NotificacionEntregaService.sanearParaMensaje(String, int).
            pendiente.setComentario(limpiarParaGuardar(texto, MAX_LARGO_COMENTARIO));
            pendiente.setMensajeCrudo(recortar(texto, MAX_LARGO_MENSAJE_CRUDO));
            pendiente.setFechaRespuesta(new Date());

            if (!calificacionDao.registrarCalificacion(pendiente, ACCION_GUARDAR_PUNTAJE)) {
                logger.warn("No se pudo guardar el puntaje {} de {} sobre la encuesta id={} "
                          + "(docEntry={} db={}).",
                            puntaje, chat, pendiente.getIdCalificacion(),
                            pendiente.getDocEntry(), pendiente.getDb());
                return false;
            }

            logger.info("Calificación {}/{} guardada para la entrega docEntry={} db={} (encuesta id={}, chat={}).",
                        puntaje, Integer.valueOf(PUNTAJE_MAX), pendiente.getDocEntry(), pendiente.getDb(),
                        pendiente.getIdCalificacion(), chat);

            acusarRecibo(chat, puntaje);
            return true;

        } catch (Throwable t) {
            // El webhook tiene que devolver 200 igual: un 500 hace que openWA reintente y
            // termine duplicando el procesamiento.
            logger.error("Error al procesar la respuesta de calificación del chat {}: {}",
                         chatId, t.getMessage(), t);
            return false;
        }
    }

    /**
     * Devuelve el puntaje 1..5 que hay en el texto de una respuesta de WhatsApp, o {@code null}
     * si no hay ninguno del que se pueda estar seguro.
     *
     * <h3>La regla de oro: ante la duda, {@code null}</h3>
     * Un {@code null} solo cuesta una calificación perdida, que además queda logueada. Un puntaje
     * inventado ensucia el promedio de un chofer y nadie se entera nunca. Por eso, si el texto
     * tiene más de un candidato distinto, o un número que no pertenece a la escala, el método
     * devuelve {@code null} en vez de elegir.
     *
     * <h3>Qué reconoce</h3>
     * <table border="1" summary="casos reconocidos">
     *   <tr><td>{@code "5"} / {@code " 5 "} / {@code "5!"}</td><td>5 — dígito suelto, con o sin
     *       espacios ni puntuación</td></tr>
     *   <tr><td>{@code "cinco"}</td><td>5 — número escrito con letra, con o sin acentos</td></tr>
     *   <tr><td>{@code "5/5"}, {@code "4 de 5"}, {@code "4 sobre 5"}</td><td>el numerador — el 5
     *       de la derecha es la escala, no un segundo voto</td></tr>
     *   <tr><td>{@code "un 4"}</td><td>4 — "un" es artículo, no el número 1 (ver
     *       {@link #PALABRAS_NUMERO})</td></tr>
     *   <tr><td>{@code "4 estrellas"}</td><td>4</td></tr>
     *   <tr><td>{@code "⭐⭐⭐⭐"}</td><td>4 — se cuentan las estrellas LLENAS; "⭐⭐⭐☆☆" da 3</td></tr>
     *   <tr><td>{@code "5️⃣"}</td><td>5 — el emoji de teclado lleva adentro el dígito real</td></tr>
     * </table>
     *
     * <h3>Qué rechaza, y por qué</h3>
     * <ul>
     *   <li><b>{@code "0"}, {@code "6"}, {@code "cero"}, {@code "siete"}</b> — fuera de la escala.
     *       Un 0 no es "muy malo": es un mensaje que no entendimos.</li>
     *   <li><b>Cualquier número de más de un dígito</b> ({@code "15"}, {@code "2024"},
     *       {@code "78888274"}) — no existe puntaje de dos dígitos, así que su presencia significa
     *       que el mensaje habla de otra cosa: un año, un teléfono, un importe. No se ignora el
     *       número: se descarta el mensaje entero.</li>
     *   <li><b>Fechas</b> ({@code "11/05/2026"}) — caen por la regla anterior. Ojo con
     *       {@code "5/5/2026"}: la fracción exige coincidencia completa, así que no entra por ahí
     *       y termina descartada por el 2026.</li>
     *   <li><b>Dos candidatos distintos</b> ({@code "3 y 4"}, {@code "2 ⭐⭐⭐⭐⭐"}) — no se
     *       adivina. En cambio {@code "5 5"} o {@code "5 ⭐⭐⭐⭐⭐"} sí resuelven: todos los
     *       candidatos valen lo mismo.</li>
     *   <li><b>Texto sin números</b> ({@code "excelente"}, {@code "pésimo"}, {@code "👍"}) —
     *       mapear adjetivos a números es adivinar el tono de alguien. No se hace.</li>
     *   <li><b>Prosa con un número adentro</b> ({@code "gracias, llegó a las 3 de la tarde"}) —
     *       un número solo cuenta si el mensaje tiene hasta {@value #MAX_LARGO_CON_NUMERO}
     *       caracteres; ver {@link #MAX_LARGO_CON_NUMERO}. Las estrellas están exentas de ese
     *       límite ({@link #MAX_LARGO_CON_ESTRELLAS}) porque no se escriben por accidente.</li>
     * </ul>
     *
     * <p>Es {@code static} y sin estado: no toca la configuración ni la base, así que se puede
     * probar sola y usar desde cualquier lado.
     *
     * @param texto texto crudo del mensaje entrante; puede ser null
     * @return un entero entre {@value #PUNTAJE_MIN} y {@value #PUNTAJE_MAX}, o {@code null}
     */
    /**
     * Palabras que pueden acompañar a un puntaje sin volverlo sospechoso.
     *
     * <p>Es una lista BLANCA corta y no una lista negra de palabras prohibidas: las formas de
     * escribir "estoy llegando en 5 minutos" son infinitas y las de escribir una calificación
     * son cuatro. Enumerar lo permitido es lo único que se puede mantener.
     *
     * <p>Los números en letra ("cinco") no van acá: los resuelve su propio paso y se descuentan
     * antes de esta comprobación.
     */
    private static final java.util.Set<String> PALABRAS_OK = new java.util.HashSet<String>(
            java.util.Arrays.asList(
                    "un", "una", "uno", "le", "les", "doy", "pongo", "es", "seria", "de",
                    "estrella", "estrellas", "punto", "puntos", "puntaje", "nota", "calificacion",
                    "califico", "calificacion", "gracias", "muchas", "ok", "listo", "todo", "bien",
                    "excelente", "buena", "buen", "bueno", "sobre", "y", "el", "la"));

    /**
     * ¿El mensaje, quitándole los números, contiene únicamente palabras compatibles con una
     * calificación?
     *
     * <p>Un mensaje vacío de letras ("5", "5!", "⭐⭐⭐⭐") pasa: es la respuesta que se pidió.
     */
    private static boolean soloPalabrasDeCalificacion(String plano) {
        // Todo lo que no sea letra se vuelve separador: así "5/5", "4-estrellas" y "un 4!"
        // quedan reducidos a sus palabras.
        String[] palabras = plano.replaceAll("[^a-záéíóúñ]+", " ").trim().split("\\s+");
        for (int i = 0; i < palabras.length; i++) {
            String w = palabras[i];
            if (w.isEmpty()) {
                continue;
            }
            if (!PALABRAS_OK.contains(w) && !PALABRAS_NUMERO_TEXTO.contains(w)) {
                return false;
            }
        }
        return true;
    }

    /** Los números escritos con letra, para que no los rechace {@link #soloPalabrasDeCalificacion}. */
    private static final java.util.Set<String> PALABRAS_NUMERO_TEXTO = new java.util.HashSet<String>(
            java.util.Arrays.asList("uno", "dos", "tres", "cuatro", "cinco"));

    public static Integer extraerPuntaje(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.isEmpty() || limpio.length() > MAX_LARGO_CON_ESTRELLAS) {
            return null;
        }

        // Las estrellas se cuentan primero porque son las que deciden con qué techo de largo se
        // evalúa el mensaje: un texto largo CON estrellas sigue siendo una calificación; un texto
        // largo con un número suelto, no.
        int estrellas = contarEstrellasLlenas(limpio);
        if (estrellas == 0 && limpio.length() > MAX_LARGO_CON_NUMERO) {
            return null;
        }

        // Se trabaja sobre el texto en minúsculas y SIN acentos, para que "cuatro" y "cuátro"
        // sean lo mismo. Los dígitos y las estrellas no se ven afectados por el plegado.
        String plano = quitarAcentos(limpio.toLowerCase(Locale.ROOT));

        // El largo por sí solo no alcanza como filtro. "estoy en el piso 3" son 18 caracteres y
        // un único dígito de la escala: pasaba el techo de 30 y quedaba guardado como un 3.
        // Igual "llego en 5 min", "son 4 cajas" o "el lunes 2". Todos mensajes cortos, con un
        // número válido, que no son calificaciones.
        //
        // La regla que se agrega: además del número tiene que haber SOLO palabras compatibles
        // con calificar. Cualquier palabra ajena tumba el mensaje.
        //
        // El sesgo es deliberado y va hacia perder respuestas antes que inventarlas: una
        // calificación que no se registra deja un hueco en el reporte; una inventada lo
        // ENSUCIA, y encima se la atribuye a un chofer que no hizo nada para merecerla. Si el
        // cliente escribió algo raro, el mensaje queda igual en mensajeCrudo para que alguien
        // lo mire a mano.
        if (!soloPalabrasDeCalificacion(plano)) {
            return null;
        }

        // Paso 1 — "4/5", "5 de 5". Va primero porque para el barrido de dígitos serían dos
        // números y la regla de "no adivinar" los descartaría. Solo aplica si NO hay estrellas:
        // con estrellas de por medio ("⭐⭐ 4/5") hay dos votos que pueden contradecirse, y eso
        // lo resuelve —descartando— la regla general de candidatos.
        if (estrellas == 0) {
            Matcher fraccion = PATRON_FRACCION.matcher(plano);
            if (fraccion.matches()) {
                return Integer.valueOf(fraccion.group(1).charAt(0) - '0');
            }
        }

        Set<Integer> candidatos = new HashSet<Integer>();

        // Paso 2 — dígitos. Cada grupo de dígitos consecutivos es un candidato; uno solo que no
        // pertenezca a la escala tumba el mensaje entero.
        int i = 0;
        int largo = plano.length();
        while (i < largo) {
            char c = plano.charAt(i);
            if (c < '0' || c > '9') {
                i++;
                continue;
            }
            int inicio = i;
            while (i < largo && plano.charAt(i) >= '0' && plano.charAt(i) <= '9') {
                i++;
            }
            if (i - inicio > 1) {
                // Año, teléfono, importe, "15". El mensaje habla de otra cosa.
                return null;
            }
            int valor = plano.charAt(inicio) - '0';
            if (valor < PUNTAJE_MIN || valor > PUNTAJE_MAX) {
                return null;
            }
            candidatos.add(Integer.valueOf(valor));
        }

        // Paso 3 — números escritos con letra.
        for (Map.Entry<Pattern, Integer> entrada : PALABRAS_NUMERO.entrySet()) {
            if (entrada.getKey().matcher(plano).find()) {
                int valor = entrada.getValue().intValue();
                if (valor < PUNTAJE_MIN || valor > PUNTAJE_MAX) {
                    return null;
                }
                candidatos.add(entrada.getValue());
            }
        }

        // Paso 4 — estrellas, ya contadas arriba sobre el texto ORIGINAL.
        if (estrellas > 0) {
            if (estrellas > PUNTAJE_MAX) {
                return null;
            }
            candidatos.add(Integer.valueOf(estrellas));
        }

        if (candidatos.size() != 1) {
            // 0 candidatos = no era una calificación. Más de uno = "3 y 4": no se adivina.
            return null;
        }
        return candidatos.iterator().next();
    }

    // =====================================================================
    // Internos
    // =====================================================================

    /**
     * Le contesta al cliente que su calificación quedó registrada.
     *
     * <p>Respeta el mismo flag de simulación que {@code NotificacionEntregaService}: con
     * {@code openwa.entregas.simular=true} no sale nada hacia openWA, se loguea el texto.
     *
     * <p>Solo se manda el número. El comentario del cliente NO vuelve en el acuse: es texto que
     * escribió alguien de afuera y devolverlo firmado por la empresa sería un canal de reflexión
     * de contenido arbitrario.
     */
    private void acusarRecibo(String chatId, Integer puntaje) {
        if (!enviarAcuse) {
            return;
        }
        // Misma puerta que el aviso de entrega. Ver el comentario de notificarCliente.
        if (!notificarCliente) {
            logger.debug("Acuse de calificación omitido: notificar-cliente=false (chatId={}).", chatId);
            return;
        }
        String mensaje = "¡Gracias por calificarnos! 🙌 Registramos su puntaje de *"
                + puntaje + "/" + PUNTAJE_MAX + "*.";
        if (simular) {
            logger.info("[SIMULACIÓN] NO se envía el acuse a {}. Texto: {}", chatId, mensaje);
            return;
        }
        try {
            if (!whatsAppService.enviarMensajeACliente(chatId, mensaje)) {
                logger.warn("openWA no aceptó el acuse de la calificación para {}; el puntaje YA quedó "
                          + "guardado, así que no se reintenta.", chatId);
            }
        } catch (Throwable t) {
            // El puntaje ya está en la base. El acuse es cortesía; que falle no cambia nada.
            logger.error("Error al enviar el acuse de calificación a {}: {}", chatId, t.getMessage(), t);
        }
    }

    /**
     * Convierte la fecha de entrega que la ACCIÓN 'G' devuelve ya formateada
     * ({@code dd/MM/yyyy HH:mm}) al {@link Date} que guarda la fila.
     *
     * <p>{@link SimpleDateFormat} no es thread-safe y esto corre en el pool de notificaciones,
     * así que se instancia uno por llamada. Es una vez por entrega: no hay nada que optimizar.
     *
     * @return la fecha, o {@code null} si venía vacía o con otro formato (el SP la deja lista, así
     *         que un null acá significa que alguien cambió la ACCIÓN 'G')
     */
    private static Date parsearFechaEntrega(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        SimpleDateFormat formato = new SimpleDateFormat(FORMATO_FECHA_ENTREGA);
        formato.setLenient(false);
        formato.setTimeZone(TimeZone.getTimeZone(ZONA_HORARIA));
        try {
            return formato.parse(valor.trim());
        } catch (ParseException e) {
            logger.warn("La fecha de entrega '{}' no tiene el formato {} que devuelve la ACCIÓN 'G'; "
                      + "la encuesta se guarda sin fecha de entrega.", valor, FORMATO_FECHA_ENTREGA);
            return null;
        }
    }

    /**
     * Deja un texto escrito por el cliente en condiciones de GUARDARSE (no de enviarse).
     *
     * <p>Colapsa saltos de línea y caracteres de control en espacios simples y recorta. No quita
     * el markdown de WhatsApp ni descarta enlaces, a diferencia de
     * {@code NotificacionEntregaService.sanearParaMensaje(String, int)}: eso es para texto que
     * SALE hacia un cliente firmado por la empresa. Este texto entra y se archiva. Si algún día
     * el comentario se muestra dentro de un mensaje saliente, hay que pasarlo por aquel método.
     */
    private static String limpiarParaGuardar(String valor, int largoMaximo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        return recortar(limpio, largoMaximo);
    }

    /**
     * Recorta sin agregar ningún carácter. No se le pone "…" a propósito: si la columna de la base
     * resultara ser {@code varchar} y no {@code nvarchar}, ese carácter se guardaría como '?'.
     */
    private static String recortar(String valor, int largoMaximo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() <= largoMaximo) {
            return limpio;
        }
        return limpio.substring(0, largoMaximo).trim();
    }

    /**
     * Plegado de acentos: "cuátro" -&gt; "cuatro". Descompone en NFD y borra las marcas
     * combinantes, que es la receta estándar y no depende de ninguna librería externa.
     */
    private static String quitarAcentos(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                         .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Cuenta las estrellas LLENAS del texto.
     *
     * <p>Recorre por code points y no por {@code char} porque 🌟 (U+1F31F) está fuera del BMP y
     * ocupa dos {@code char} en Java: un recorrido ingenuo la contaría dos veces o ninguna.
     */
    private static int contarEstrellasLlenas(String texto) {
        int total = 0;
        int i = 0;
        while (i < texto.length()) {
            int cp = texto.codePointAt(i);
            if (cp == ESTRELLA_LLENA_BLANCA || cp == ESTRELLA_LLENA_NEGRA || cp == ESTRELLA_BRILLANTE) {
                total++;
            }
            i += Character.charCount(cp);
        }
        return total;
    }
}
