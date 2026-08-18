package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.CalificacionService;
import bo.bosque.com.impexpap.commons.WhatsAppService;
import bo.bosque.com.impexpap.dao.ICalificacionEntrega;
import bo.bosque.com.impexpap.dto.ResumenCalificacionDTO;
import bo.bosque.com.impexpap.model.CalificacionEntrega;
import bo.bosque.com.impexpap.model.EntregaChofer;
import bo.bosque.com.impexpap.utils.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Entrada de los eventos de openWA (webhook público) y reportes de las calificaciones de entrega.
 *
 * <p>Tres endpoints que no se parecen en nada entre sí, y conviven acá porque son las tres puntas
 * del mismo feature:</p>
 * <ul>
 *   <li>{@code POST /whatsapp/webhook} — <b>público</b>, sin JWT, autenticado por firma HMAC.
 *       Lo llama openWA cuando un cliente responde por WhatsApp.</li>
 *   <li>{@code POST /entregas/calificaciones} — listado, JWT normal.</li>
 *   <li>{@code POST /entregas/calificaciones-resumen} — promedio y distribución por chofer, JWT
 *       normal.</li>
 * </ul>
 *
 * <h2>1. Cómo se registra el webhook contra openWA</h2>
 * Hay que hacerlo UNA vez por sesión de openWA (y de nuevo si la sesión se recrea). No hay UI:
 * es un POST al propio openWA. El {@code secret} que se manda acá tiene que ser EXACTAMENTE el
 * mismo valor de {@code openwa.webhook.secret} / variable de entorno
 * {@code OPENWA_WEBHOOK_SECRET} del backend, porque es con ese string que se firma y se verifica
 * cada request.
 *
 * <h3>PowerShell (desde una PC de la red)</h3>
 * <pre>
 * $secreto = "PONER-UN-SECRETO-LARGO-Y-ALEATORIO"
 * $body = @{
 *     url        = "http://181.114.119.195:9223/whatsapp/webhook"
 *     events     = @("message.received")
 *     secret     = $secreto
 *     retryCount = 3
 * } | ConvertTo-Json
 *
 * Invoke-RestMethod -Method Post `
 *   -Uri "http://181.114.119.195:2785/api/sessions/58812227-7ca1-4205-b5c2-7dbc01870fc9/webhooks" `
 *   -Headers @{ "X-API-Key" = "<OPENWA_API_KEY>" } `
 *   -ContentType "application/json" -Body $body
 * </pre>
 *
 * <h3>curl (desde el servidor, que es lo que corresponde en producción)</h3>
 * <pre>
 * curl -X POST \
 *   "http://host.containers.internal:2785/api/sessions/58812227-7ca1-4205-b5c2-7dbc01870fc9/webhooks" \
 *   -H "X-API-Key: <OPENWA_API_KEY>" \
 *   -H "Content-Type: application/json" \
 *   -d '{"url":"http://host.containers.internal:9223/whatsapp/webhook",
 *        "events":["message.received"],
 *        "secret":"PONER-UN-SECRETO-LARGO-Y-ALEATORIO",
 *        "retryCount":3}'
 * </pre>
 *
 * <p><b>Qué URL poner.</b> La URL la resuelve openWA, que corre dentro de un contenedor: tiene
 * que ser alcanzable DESDE EL CONTENEDOR, no desde la PC del que registra. Si el backend corre en
 * el mismo host que el contenedor, eso es
 * {@code http://host.containers.internal:9223/whatsapp/webhook} (9223 es {@code server.port}). La
 * variante con la IP pública sirve para probar desde afuera. Es el mismo par de valores que ya
 * tiene {@code openwa.url} en {@code application.properties}, en espejo.</p>
 *
 * <p><b>La URL NO puede llevar query string.</b> El WAF casero ({@code security.SecurityFilter})
 * evalúa la query string entera contra un patrón que rechaza cualquier comilla y las palabras
 * {@code union|select|from|where|drop}. Un {@code ?token=...} con un apóstrofe adentro devolvería
 * 403 y el síntoma no diría nada. Todo lo que hace falta viaja por header y por body.</p>
 *
 * <h3>Verificar y probar el registro</h3>
 * <pre>
 * GET    /api/sessions/{sessionId}/webhooks              -&gt; lista los webhooks de la sesión
 * POST   /api/sessions/{sessionId}/webhooks/{id}/test    -&gt; dispara un evento de prueba
 * DELETE /api/sessions/{sessionId}/webhooks/{id}         -&gt; lo borra
 * </pre>
 *
 * <h3>Probar la firma sin openWA (PowerShell)</h3>
 * <pre>
 * $secreto = "PONER-UN-SECRETO-LARGO-Y-ALEATORIO"
 * $body = '{"event":"message.received","data":{"from":"59171234567@c.us","body":"5","fromMe":false}}'
 * $hmac = New-Object System.Security.Cryptography.HMACSHA256
 * $hmac.Key = [Text.Encoding]::UTF8.GetBytes($secreto)
 * $firma = (($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($body)) |
 *            ForEach-Object { $_.ToString("x2") }) -join '')
 *
 * Invoke-RestMethod -Method Post -Uri "http://localhost:9223/whatsapp/webhook" `
 *   -Headers @{ "X-Webhook-Signature" = "sha256=$firma" } `
 *   -ContentType "application/json" -Body $body
 * </pre>
 * (usar un body ASCII para la prueba: si se le meten acentos, hay que asegurarse de que
 * PowerShell lo mande en UTF-8, porque si no la firma se calcula sobre otros bytes.)
 *
 * <h2>2. Lo que hay que tocar afuera de esta clase para que funcione</h2>
 * <ul>
 *   <li><b>{@code security.MainSecurity}</b>: agregar
 *       {@code .antMatchers("/whatsapp/webhook").permitAll()} ANTES de
 *       {@code .anyRequest().authenticated()}. Puesto después no tiene efecto y el síntoma es un
 *       401 silencioso del {@code JwtEntryPoint} que parece un problema de openWA. openWA no
 *       puede mandar un JWT: la autenticación de este endpoint es la firma HMAC, no el token.</li>
 *   <li><b>{@code application.properties}</b>: {@code openwa.webhook.secret},
 *       {@code openwa.webhook.habilitado}, {@code openwa.entregas.pedir-calificacion} y
 *       {@code openwa.entregas.calificacion-ventana-horas}.</li>
 * </ul>
 * Este controlador NO lleva {@code @Secured} ni {@code @PreAuthorize} a nivel de clase: con
 * {@code @EnableGlobalMethodSecurity} activo, cualquiera de las dos anularía el {@code permitAll}
 * y devolvería 403 después de haber entrado sin token. Los dos endpoints de reporte sí llevan
 * {@code @Secured} a nivel de método, como el resto del módulo.
 *
 * <h2>3. Códigos de respuesta, y por qué</h2>
 * <ul>
 *   <li><b>404</b> si {@code openwa.webhook.habilitado=false} — apagado es indistinguible de
 *       inexistente.</li>
 *   <li><b>401</b> si no hay secreto configurado, si falta la firma o si no coincide.</li>
 *   <li><b>200 en TODO el resto</b>, incluso cuando el JSON es incomprensible o el
 *       procesamiento falla. openWA reintenta hasta {@code retryCount} veces ante un 5xx: un 500
 *       por un mensaje que igual nunca se va a poder procesar es una tormenta de reintentos, y en
 *       el peor caso una calificación duplicada.</li>
 * </ul>
 *
 * <h2>4. Si la firma no valida en producción, mirar esto antes que nada</h2>
 * <ol>
 *   <li><b>Content-Type</b>. Si openWA mandara {@code application/x-www-form-urlencoded},
 *       Tomcat parsea y CONSUME el body al primer {@code getParameterMap()} — que lo hace el WAF,
 *       antes del controlador — y acá llega vacío. La firma nunca podría coincidir. Tiene que ser
 *       {@code application/json}.</li>
 *   <li><b>Sobre qué se firma</b>. Acá se prueba el body crudo y, si viene un header de
 *       timestamp, también {@code timestamp + "." + body} y {@code timestamp + body}, que son las
 *       variantes habituales. Si openWA usara otro esquema, hay que agregarlo en
 *       {@link #firmaValida(byte[], String, String)}.</li>
 *   <li><b>User-Agent</b>. El WAF rechaza con 403 cualquiera que contenga {@code python-requests}
 *       (y sqlmap, nikto, nessus, nmap, burpsuite, ZAP, masscan). Si se prueba el webhook con
 *       {@code requests} de Python, el 403 no tiene nada que ver con la firma.</li>
 * </ol>
 */
@RestController
@CrossOrigin("*")
public class WhatsAppWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    /**
     * Parser propio, y no el {@code ObjectMapper} de Spring, porque acá se lee JSON de origen
     * desconocido a un árbol genérico: no hace falta —ni conviene— arrastrar la configuración de
     * fechas de {@code config.JacksonConfig}. {@code ObjectMapper} es thread-safe para lectura.
     */
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String ALGORITMO_HMAC = "HmacSHA256";

    /** Largo en bytes de un HMAC-SHA256. Sirve para descartar firmas con formato imposible. */
    private static final int LARGO_HMAC_BYTES = 32;

    /**
     * Headers donde puede venir la firma, en orden de preferencia. El body de openWA no está
     * documentado y el nombre del header tampoco: se prueban los habituales y gana el primero que
     * traiga algo. {@code getHeader} del servlet es case-insensitive, así que no hace falta
     * repetir variantes de mayúsculas.
     */
    private static final String[] HEADERS_FIRMA = {
            "X-Webhook-Signature",
            "X-Hub-Signature-256",
            "X-Signature-256",
            "X-Signature",
            "X-Openwa-Signature"
    };

    /** Headers donde puede venir el instante de emisión, para el control de replay. */
    private static final String[] HEADERS_TIMESTAMP = {
            "X-Webhook-Timestamp",
            "X-Hub-Timestamp",
            "X-Signature-Timestamp",
            "X-Timestamp"
    };

    /**
     * Desfase máximo tolerado entre el timestamp del header y la hora del servidor, en
     * milisegundos. Solo se evalúa si el header VIENE: openWA puede no mandarlo, y en ese caso no
     * hay control de replay posible. Vale la pena igual, porque el daño de un replay es acotado:
     * la primera vez la encuesta pendiente se completa, y a partir de ahí ya no hay fila
     * pendiente que actualizar.
     */
    private static final long MAX_DESFASE_MS = 5L * 60L * 1000L;

    /**
     * Rutas (JSON Pointer) donde puede estar el chat del que llegó el mensaje. Se prueban en
     * orden y gana la primera que traiga un valor.
     */
    private static final String[] RUTAS_CHAT = {
            "/data/from", "/from", "/payload/from",
            "/data/chatId", "/chatId", "/payload/chatId",
            "/data/message/from", "/message/from",
            "/data/key/remoteJid", "/key/remoteJid",
            "/data/sender", "/sender"
    };

    /** Rutas donde puede estar el texto del mensaje. */
    private static final String[] RUTAS_TEXTO = {
            "/data/body", "/body", "/payload/body",
            "/data/text", "/text", "/payload/text",
            "/data/message/body", "/message/body",
            "/data/message/conversation", "/message/conversation",
            "/data/caption", "/caption"
    };

    /** Rutas donde puede estar la marca de "lo mandé yo". */
    private static final String[] RUTAS_FROM_ME = {
            "/data/fromMe", "/fromMe", "/payload/fromMe",
            "/data/key/fromMe", "/key/fromMe",
            "/data/message/fromMe", "/message/fromMe"
    };

    /**
     * Rutas donde puede estar el nombre del EVENTO ({@code message.received}, {@code session.status},
     * …).
     *
     * <p><b>{@code /type} no está en la lista, y es a propósito.</b> En los objetos de mensaje de
     * las librerías de WhatsApp, {@code type} es el tipo del MENSAJE ({@code "chat"},
     * {@code "image"}, {@code "ptt"}), no el del evento. Si openWA mandara el mensaje pelado,
     * mirar {@code /type} daría {@code "chat"}, que no empieza con "message", y el filtro
     * descartaría en silencio todas las calificaciones.
     */
    private static final String[] RUTAS_EVENTO = {
            "/event", "/eventType", "/data/event", "/data/eventType"
    };

    /** Largo al que se recorta el JSON entrante cuando hay que loguearlo entero. */
    private static final int MAX_LOG_CUERPO = 2000;

    private final CalificacionService calificacionService;
    private final ICalificacionEntrega calificacionDao;

    /**
     * Secreto compartido con openWA. <b>Vacío = el endpoint rechaza todo.</b> No hay "modo
     * abierto": sin firma, cualquiera que conozca la URL puede inyectar calificaciones falsas a
     * nombre de cualquier cliente. Mismo criterio de lista blanca que ya usan
     * {@code openwa.entregas.sap-bases} y {@code openwa.entregas.tipos-con-aviso}.
     */
    @Value("${openwa.webhook.secret:}")
    private String webhookSecret;

    /** Interruptor del endpoint. Apagado = 404. */
    @Value("${openwa.webhook.habilitado:false}")
    private boolean webhookHabilitado;

    /**
     * Número del propio bot, para descartar los mensajes que manda el backend. Es la misma
     * propiedad que ya usa {@code WhatsAppService}.
     */
    @Value("${openwa.default-phone:59178888274@c.us}")
    private String telefonoBot;

    /** {@link #telefonoBot} normalizado a chatId, resuelto una sola vez al arrancar. */
    private String chatIdBot;

    public WhatsAppWebhookController(CalificacionService calificacionService,
                                     ICalificacionEntrega calificacionDao) {
        this.calificacionService = calificacionService;
        this.calificacionDao = calificacionDao;
    }

    /**
     * Deja constancia en el arranque del estado del webhook. Un webhook encendido sin secreto es
     * un endpoint público que rechaza todo: hay que verlo en el log y no descubrirlo mirando por
     * qué openWA recibe 401.
     */
    @PostConstruct
    void anunciarModo() {
        chatIdBot = WhatsAppService.normalizarChatId(telefonoBot);
        if (!webhookHabilitado) {
            LOG.info("Webhook de WhatsApp APAGADO (openwa.webhook.habilitado=false): "
                   + "POST /whatsapp/webhook responde 404.");
            return;
        }
        if (secretoVacio()) {
            LOG.error("Webhook de WhatsApp ENCENDIDO pero SIN SECRETO (openwa.webhook.secret vacío): "
                    + "todas las peticiones se van a rechazar con 401. Configurar la variable de "
                    + "entorno OPENWA_WEBHOOK_SECRET con el mismo valor que se le pasó a openWA al "
                    + "registrar el webhook.");
            return;
        }
        LOG.warn("Webhook de WhatsApp ACTIVO en POST /whatsapp/webhook, con verificación HMAC-SHA256. "
               + "Recordá que la ruta tiene que estar en permitAll de MainSecurity. Bot: {}.", chatIdBot);
    }

    // =====================================================================
    // Webhook público
    // =====================================================================

    /**
     * Recibe un evento {@code message.received} de openWA y, si el mensaje es una calificación,
     * la guarda.
     *
     * <p><b>El body se recibe como {@code byte[]} y no como un DTO.</b> No es un capricho: la
     * firma HMAC se calcula sobre los BYTES EXACTOS que mandó openWA. Si Jackson deserializara a
     * un objeto, volver a serializarlo daría otro texto (orden de claves, espacios, escapes de
     * Unicode) y la firma jamás coincidiría. Tampoco sirve {@code @RequestBody String}: el
     * {@code StringHttpMessageConverter} de Spring decodifica con ISO-8859-1 cuando el
     * Content-Type no trae {@code charset}, y con eso cualquier acento o emoji rompe el HMAC.
     *
     * @param cuerpo  body crudo; puede llegar null si el request viene sin cuerpo
     * @param request se usa solo para leer headers (nombres case-insensitive) y la IP
     */
    @PostMapping("/whatsapp/webhook")
    public ResponseEntity<ApiResponse<String>> recibirEventoWhatsApp(
            @RequestBody(required = false) byte[] cuerpo,
            HttpServletRequest request) {

        // 1. Apagado = inexistente. Va primero: ni siquiera se mira la firma.
        if (!webhookHabilitado) {
            return respuesta(HttpStatus.NOT_FOUND, "No encontrado", null);
        }

        String ip = ipCliente(request);

        // 2. Sin secreto no se procesa NADA. Sin firma verificable, cualquiera podría inyectar
        //    calificaciones falsas a nombre de cualquier cliente.
        if (secretoVacio()) {
            LOG.error("Llegó un evento al webhook desde {} pero openwa.webhook.secret está vacío: "
                    + "se rechaza con 401. Configurar OPENWA_WEBHOOK_SECRET.", ip);
            return respuesta(HttpStatus.UNAUTHORIZED, "Webhook sin secreto configurado", null);
        }

        byte[] cuerpoCrudo = (cuerpo == null) ? new byte[0] : cuerpo;

        // 3. Firma.
        String firmaRecibida = primerHeader(request, HEADERS_FIRMA);
        if (firmaRecibida == null) {
            LOG.warn("Evento del webhook SIN header de firma desde {} (probados: {}). Se rechaza. "
                   + "Verificar que el webhook se haya registrado en openWA con 'secret'.",
                     ip, String.join(", ", HEADERS_FIRMA));
            return respuesta(HttpStatus.UNAUTHORIZED, "Falta la firma", null);
        }

        String timestamp = primerHeader(request, HEADERS_TIMESTAMP);
        if (!timestampFresco(timestamp)) {
            LOG.warn("Evento del webhook con timestamp fuera de la ventana de {} min desde {} "
                   + "(recibido '{}'). Se rechaza por posible reenvío.",
                     Long.valueOf(MAX_DESFASE_MS / 60000L), ip, timestamp);
            return respuesta(HttpStatus.UNAUTHORIZED, "Timestamp fuera de ventana", null);
        }

        if (!firmaValida(cuerpoCrudo, firmaRecibida, timestamp)) {
            // Nunca se loguea el secreto, y de la firma recibida solo un prefijo: alcanza para
            // distinguir "manda otro formato" de "manda otro secreto".
            LOG.warn("Firma inválida en el webhook desde {} (firma recibida empieza con '{}', {} bytes "
                   + "de body). Se rechaza con 401.",
                     ip, prefijo(firmaRecibida, 12), Integer.valueOf(cuerpoCrudo.length));
            return respuesta(HttpStatus.UNAUTHORIZED, "Firma inválida", null);
        }

        // 4. A partir de acá SIEMPRE se responde 200: el request es auténtico, y devolver un
        //    error solo lograría que openWA lo reintente hasta retryCount veces.
        try {
            return procesarEventoFirmado(cuerpoCrudo, ip);
        } catch (Throwable t) {
            LOG.error("Error procesando un evento del webhook desde {}: {}", ip, t.getMessage(), t);
            return respuesta(HttpStatus.OK, "Recibido con errores", "error");
        }
    }

    /**
     * Interpreta el JSON ya autenticado. Todo lo que no se entiende termina en un WARN con el
     * cuerpo completo y un 200: es la única forma de poder ajustar las rutas de búsqueda cuando
     * se vea el formato real que manda openWA.
     */
    private ResponseEntity<ApiResponse<String>> procesarEventoFirmado(byte[] cuerpo, String ip) {
        JsonNode raiz;
        try {
            raiz = JSON.readTree(cuerpo);
        } catch (Exception e) {
            LOG.warn("El webhook recibió un cuerpo que no es JSON válido desde {}: {}. Cuerpo: {}",
                     ip, e.getMessage(), textoDelCuerpo(cuerpo));
            return respuesta(HttpStatus.OK, "Cuerpo no interpretable", "ignorado");
        }
        if (raiz == null || raiz.isNull()) {
            LOG.warn("El webhook recibió un cuerpo vacío desde {}.", ip);
            return respuesta(HttpStatus.OK, "Cuerpo vacío", "ignorado");
        }

        // Solo interesan los mensajes entrantes. Si el evento viene identificado y no habla de
        // mensajes (status, session.*, etc.), se descarta sin ruido.
        String evento = buscarValor(raiz, RUTAS_EVENTO);
        if (evento != null && !evento.toLowerCase(Locale.ROOT).startsWith("message")) {
            LOG.debug("Evento '{}' del webhook: no es un mensaje entrante, se ignora.", evento);
            return respuesta(HttpStatus.OK, "Evento ignorado", "ignorado");
        }

        // Mensajes propios: el backend le escribe al cliente y openWA nos devuelve el eco. Sin
        // este filtro, el acuse "¡Gracias por calificarnos! ... 5/5" volvería como un mensaje con
        // un número adentro y el bot terminaría calificándose a sí mismo.
        if (esVerdadero(raiz, RUTAS_FROM_ME)) {
            LOG.debug("Mensaje propio (fromMe) en el webhook: se ignora.");
            return respuesta(HttpStatus.OK, "Mensaje propio", "ignorado");
        }

        String chat = buscarValor(raiz, RUTAS_CHAT);
        String texto = buscarValor(raiz, RUTAS_TEXTO);
        if (chat == null || texto == null) {
            // Caso esperable el primer día: hasta no ver un evento real no se sabe dónde vienen
            // los campos. Por eso se loguea el JSON entero, y por eso se responde 200: que openWA
            // reintente cinco veces no va a hacer que el formato cambie.
            LOG.warn("No se pudo ubicar {} en el evento del webhook desde {}. Agregar la ruta que "
                   + "corresponda en RUTAS_CHAT / RUTAS_TEXTO. JSON recibido: {}",
                     chat == null ? (texto == null ? "el chat ni el texto" : "el chat") : "el texto",
                     ip, textoDelCuerpo(cuerpo));
            return respuesta(HttpStatus.OK, "Evento sin datos utilizables", "ignorado");
        }

        if (chatIdBot != null && chatIdBot.equalsIgnoreCase(chat)) {
            LOG.debug("Mensaje cuyo remitente es el propio bot ({}): se ignora.", chat);
            return respuesta(HttpStatus.OK, "Mensaje propio", "ignorado");
        }
        if (chat.toLowerCase(Locale.ROOT).endsWith("@g.us")) {
            // Una encuesta es siempre un chat individual. CalificacionService lo volvería a
            // rechazar, pero cortarlo acá evita una consulta y deja el motivo en el log.
            LOG.debug("Mensaje de grupo ({}) en el webhook: se ignora.", chat);
            return respuesta(HttpStatus.OK, "Mensaje de grupo", "ignorado");
        }

        // El texto es dato NO confiable: lo escribió alguien de afuera. No se loguea entero, no
        // se concatena en SQL (el DAO lo manda por parámetro) y no vuelve a salir en ningún
        // mensaje de WhatsApp.
        boolean guardada = calificacionService.procesarRespuesta(chat, texto);
        return respuesta(HttpStatus.OK, guardada ? "Calificación registrada" : "Sin calificación",
                         guardada ? "guardada" : "ignorado");
    }

    // Los endpoints de reporte (/entregas/calificaciones y /calificaciones-resumen) NO viven
    // acá: están en EntregaChoferController, que ya tiene @RequestMapping("/entregas") de clase
    // y el resto del módulo. Tenerlos duplicados en los dos controllers hacía que Spring fallara
    // al arrancar con "Ambiguous mapping" — un choque que el compilador no ve, porque las dos
    // clases compilan perfecto y el conflicto recién aparece al registrar los handlers.
    //
    // Este controller expone UNA sola ruta, /whatsapp/webhook, y es la única del proyecto que
    // atiende sin JWT (se autentica por HMAC). Mantenerlo así de chico es a propósito.

    // =====================================================================
    // Firma
    // =====================================================================

    /** ¿Está el secreto sin configurar? */
    private boolean secretoVacio() {
        return webhookSecret == null || webhookSecret.trim().isEmpty();
    }

    /**
     * Compara la firma recibida contra el HMAC-SHA256 del cuerpo.
     *
     * <p>Se aceptan las tres formas en que suele venir el payload firmado, porque el esquema de
     * openWA no está documentado y probar de más no debilita nada: las tres exigen conocer el
     * secreto. Si el día de mañana se confirma cuál usa, se pueden borrar las otras dos.
     *
     * <p>La comparación es con {@link MessageDigest#isEqual(byte[], byte[])} y no con
     * {@code equals}: {@code equals} de String corta en el primer byte distinto, y ese tiempo
     * diferencial es lo que permite descubrir una firma válida byte por byte.
     */
    private boolean firmaValida(byte[] cuerpo, String firmaRecibida, String timestamp) {
        byte[] recibida = decodificarFirma(firmaRecibida);
        if (recibida == null) {
            return false;
        }
        if (coincide(hmac(cuerpo), recibida)) {
            return true;
        }
        if (timestamp != null && !timestamp.trim().isEmpty()) {
            String ts = timestamp.trim();
            if (coincide(hmac(concatenar(ts + ".", cuerpo)), recibida)) {
                return true;
            }
            if (coincide(hmac(concatenar(ts, cuerpo)), recibida)) {
                return true;
            }
        }
        return false;
    }

    /** HMAC-SHA256 del contenido con el secreto configurado. Devuelve null si algo falla. */
    private byte[] hmac(byte[] contenido) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO_HMAC);
            mac.init(new SecretKeySpec(webhookSecret.trim().getBytes(StandardCharsets.UTF_8), ALGORITMO_HMAC));
            return mac.doFinal(contenido);
        } catch (Exception e) {
            // No debería pasar nunca: HmacSHA256 es obligatorio en toda JVM y el secreto no está
            // vacío (se validó antes). Si pasa, es un problema de la instalación, no del request.
            LOG.error("No se pudo calcular el HMAC del webhook: {}", e.getMessage(), e);
            return null;
        }
    }

    /** Comparación en tiempo constante, tolerante a nulls. */
    private static boolean coincide(byte[] esperada, byte[] recibida) {
        return esperada != null && recibida != null && MessageDigest.isEqual(esperada, recibida);
    }

    /**
     * Pasa la firma del header a bytes. Acepta {@code sha256=<hex>}, {@code <hex>} y base64,
     * porque cada servidor la escribe distinto.
     *
     * @return los 32 bytes del HMAC, o null si el formato no da
     */
    private static byte[] decodificarFirma(String valor) {
        String limpio = valor.trim();
        int igual = limpio.indexOf('=');
        // "sha256=abc..." -> "abc...". Ojo: en base64 el '=' es relleno y va AL FINAL, por eso se
        // exige que lo de la izquierda sea un prefijo alfabético y no vacío.
        if (igual > 0 && limpio.substring(0, igual).matches("(?i)[a-z0-9_-]+")
                && igual < limpio.length() - 1) {
            String posiblePrefijo = limpio.substring(0, igual).toLowerCase(Locale.ROOT);
            if (posiblePrefijo.startsWith("sha") || "hmac".equals(posiblePrefijo)
                    || "signature".equals(posiblePrefijo)) {
                limpio = limpio.substring(igual + 1).trim();
            }
        }
        if (limpio.length() == LARGO_HMAC_BYTES * 2 && limpio.matches("(?i)[0-9a-f]+")) {
            byte[] bytes = new byte[LARGO_HMAC_BYTES];
            for (int i = 0; i < LARGO_HMAC_BYTES; i++) {
                bytes[i] = (byte) Integer.parseInt(limpio.substring(i * 2, i * 2 + 2), 16);
            }
            return bytes;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(limpio);
            return bytes.length == LARGO_HMAC_BYTES ? bytes : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Concatena un prefijo de texto con el cuerpo crudo, sin volver a decodificar el cuerpo. */
    private static byte[] concatenar(String prefijo, byte[] cuerpo) {
        byte[] pre = prefijo.getBytes(StandardCharsets.UTF_8);
        byte[] total = new byte[pre.length + cuerpo.length];
        System.arraycopy(pre, 0, total, 0, pre.length);
        System.arraycopy(cuerpo, 0, total, pre.length, cuerpo.length);
        return total;
    }

    /**
     * Control de reenvío (replay). Si NO viene timestamp devuelve true: no se puede verificar lo
     * que no se manda, y rechazar por eso dejaría el webhook inservible con un openWA que no lo
     * incluye. Acepta el valor en segundos o en milisegundos.
     */
    private static boolean timestampFresco(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return true;
        }
        long enviado;
        try {
            enviado = Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            // Formato desconocido (una fecha ISO, por ejemplo): no se puede evaluar, no se rechaza.
            return true;
        }
        // Menos de 100.000 millones = está en segundos (en milisegundos eso sería 1973).
        long enviadoMs = enviado < 100000000000L ? enviado * 1000L : enviado;
        return Math.abs(System.currentTimeMillis() - enviadoMs) <= MAX_DESFASE_MS;
    }

    // =====================================================================
    // Lectura defensiva del JSON
    // =====================================================================

    /**
     * Devuelve el primer valor no vacío de las rutas dadas (JSON Pointer, {@code /data/from}).
     *
     * <p>Si la ruta apunta a un OBJETO se prueban las claves con las que las librerías de WhatsApp
     * envuelven un id ({@code _serialized}, {@code id}, {@code user}), porque algunos formatos
     * mandan {@code "from": {"_serialized": "591...@c.us", ...}} en vez de un string pelado.
     */
    private static String buscarValor(JsonNode raiz, String[] rutas) {
        for (int i = 0; i < rutas.length; i++) {
            JsonNode nodo = raiz.at(rutas[i]);
            if (nodo == null || nodo.isMissingNode() || nodo.isNull()) {
                continue;
            }
            if (nodo.isObject()) {
                String anidado = primerTextoDe(nodo, "_serialized", "id", "user");
                if (anidado != null) {
                    return anidado;
                }
                continue;
            }
            if (nodo.isValueNode()) {
                String valor = nodo.asText("").trim();
                if (!valor.isEmpty()) {
                    return valor;
                }
            }
        }
        return null;
    }

    /** Primer campo de texto no vacío del objeto, entre los nombres dados. */
    private static String primerTextoDe(JsonNode objeto, String... nombres) {
        for (int i = 0; i < nombres.length; i++) {
            JsonNode hijo = objeto.get(nombres[i]);
            if (hijo != null && hijo.isValueNode()) {
                String valor = hijo.asText("").trim();
                if (!valor.isEmpty()) {
                    return valor;
                }
            }
        }
        return null;
    }

    /**
     * ¿Alguna de las rutas dice que sí? Acepta el booleano JSON y también los strings
     * {@code "true"} / {@code "1"}, que es como lo mandan algunos serializadores.
     */
    private static boolean esVerdadero(JsonNode raiz, String[] rutas) {
        for (int i = 0; i < rutas.length; i++) {
            JsonNode nodo = raiz.at(rutas[i]);
            if (nodo == null || !nodo.isValueNode()) {
                continue;
            }
            if (nodo.isBoolean() && nodo.asBoolean()) {
                return true;
            }
            String valor = nodo.asText("").trim();
            if ("true".equalsIgnoreCase(valor) || "1".equals(valor)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================

    private static <T> ResponseEntity<ApiResponse<T>> respuesta(HttpStatus estado, String mensaje, T dato) {
        return ResponseEntity.status(estado).body(new ApiResponse<T>(mensaje, dato, estado.value()));
    }

    /** Primer header presente y no vacío, entre los nombres dados. */
    private static String primerHeader(HttpServletRequest request, String[] nombres) {
        for (int i = 0; i < nombres.length; i++) {
            String valor = request.getHeader(nombres[i]);
            if (valor != null && !valor.trim().isEmpty()) {
                return valor.trim();
            }
        }
        return null;
    }

    /** IP del que llama, mirando el proxy si lo hay. Solo se usa para el log. */
    private static String ipCliente(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada == null || reenviada.trim().isEmpty()) {
            return request.getRemoteAddr();
        }
        return reenviada.split(",")[0].trim();
    }

    /**
     * El cuerpo como texto para loguearlo, recortado. Un webhook público puede recibir cualquier
     * cosa: sin el recorte, un cuerpo de 10 MB serían 10 MB de log.
     */
    private static String textoDelCuerpo(byte[] cuerpo) {
        if (cuerpo == null || cuerpo.length == 0) {
            return "(vacío)";
        }
        int largo = Math.min(cuerpo.length, MAX_LOG_CUERPO);
        String texto = new String(cuerpo, 0, largo, StandardCharsets.UTF_8);
        return cuerpo.length > MAX_LOG_CUERPO ? texto + "...(" + cuerpo.length + " bytes)" : texto;
    }

    /** Prefijo de un texto, para loguear una firma sin loguearla entera. */
    private static String prefijo(String valor, int largo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= largo ? valor : valor.substring(0, largo) + "...";
    }
}
