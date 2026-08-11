package bo.bosque.com.impexpap.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de envio de mensajes por WhatsApp a traves de openWA (HTTP).
 *
 * <p>Regla de oro de esta clase: NUNCA propaga una excepcion. Todos los envios
 * atrapan sus propios errores y los loguean. Los jobs de produccion
 * (cumpleanios, rol, cierre) dependen de este comportamiento, y el aviso de
 * entregas exige que un openWA caido jamas rompa el flujo del negocio.</p>
 *
 * <p>El {@link RestTemplate} lleva timeouts CORTOS a proposito: el default de
 * Spring es infinito, y un openWA colgado dejaria hilos del executor de
 * notificaciones parados para siempre, hasta llenar la cola y empezar a
 * rechazar tareas en el hilo del request.</p>
 */
@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    /** Timeout de conexion al openWA, en milisegundos. */
    private static final int CONNECT_TIMEOUT_MS = 3000;

    /** Timeout de lectura de la respuesta del openWA, en milisegundos. */
    private static final int READ_TIMEOUT_MS = 5000;

    /** Cantidad de digitos de un celular boliviano sin prefijo de pais. */
    private static final int LARGO_CELULAR_BO = 8;

    /** Sufijo de chat individual en WhatsApp. */
    private static final String SUFIJO_CONTACTO = "@c.us";

    /** Sufijo de chat grupal en WhatsApp. */
    private static final String SUFIJO_GRUPO = "@g.us";

    /**
     * Copia estatica del prefijo de pais configurado, para que
     * {@link #normalizarChatId(String)} pueda ser static (asi lo exige el
     * contrato compartido con los otros modulos). Se refresca en
     * {@link #init()} desde la propiedad {@code openwa.pais-prefijo}.
     * Mantiene el default "591" por si alguien llama al metodo antes de que
     * Spring termine de levantar el bean.
     */
    private static volatile String prefijoPaisEstatico = "591";

    // =====================================================
    // CONFIGURACION OPENWA (application.properties / env)
    // =====================================================
    // OJO: los campos con @Value NO pueden ser final.

    @Value("${openwa.url:http://host.containers.internal:2785}")
    private String openwaUrl;

    @Value("${openwa.session-id:58812227-7ca1-4205-b5c2-7dbc01870fc9}")
    private String sessionId;

    @Value("${openwa.api-key:dev-admin-key}")
    private String apiKey;

    @Value("${openwa.default-phone:59178888274@c.us}")
    private String defaultPhone;

    /** Lista separada por comas. Se usan como maximo 2 grupos. */
    @Value("${openwa.groups:59178888272-1422893975@g.us}")
    private String groupsConfig;

    @Value("${openwa.pais-prefijo:591}")
    private String paisPrefijo;

    private final RestTemplate restTemplate;

    public WhatsAppService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Publica el prefijo de pais configurado para el metodo estatico de
     * normalizacion y deja constancia en el log de a donde apunta el servicio.
     */
    @PostConstruct
    public void init() {
        if (paisPrefijo != null && !paisPrefijo.trim().isEmpty()) {
            prefijoPaisEstatico = paisPrefijo.trim();
        }
        logger.info("WhatsAppService listo. openWA={}, prefijoPais={}, timeouts(connect/read)={}ms/{}ms",
                openwaUrl, prefijoPaisEstatico, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
    }

    /**
     * Envia mensaje de texto a un numero o grupo.
     * Comportamiento historico: no devuelve nada y nunca lanza excepcion.
     */
    public void enviarMensaje(String texto, String chatId) {
        enviarTexto(texto, chatId);
    }

    /**
     * Envia notificacion con formato a los grupos configurados (maximo 2).
     */
    public void enviarNotificacionAGrupos(String titulo, String mensaje) {
        String texto = "🔔 *" + titulo + "*\n\n" + mensaje;

        List<String> grupos = obtenerGruposLimitados();

        if (grupos.isEmpty()) {
            logger.warn("⚠️ No hay grupos configurados en openwa.groups");
            return;
        }

        for (String grupoId : grupos) {
            enviarMensaje(texto, grupoId);
        }
    }

    /**
     * Metodo para pruebas (envia al numero por defecto).
     */
    public void enviarPrueba(String mensaje) {
        String texto = "🧪 [PRUEBA] " + mensaje;
        enviarMensaje(texto, defaultPhone);
    }

    /**
     * Envia un mensaje a un cliente a partir de un telefono en crudo
     * (con o sin prefijo de pais, con espacios, guiones o parentesis).
     *
     * @param telefonoCrudo telefono tal como viene de la base o del usuario
     * @param texto         cuerpo del mensaje
     * @return true si openWA respondio 2xx; false si el telefono era invalido
     *         o el envio fallo. Nunca lanza excepcion.
     */
    public boolean enviarMensajeACliente(String telefonoCrudo, String texto) {
        String chatId = normalizarChatId(telefonoCrudo);
        if (chatId == null) {
            logger.warn("No se pudo enviar WhatsApp: telefono invalido o vacio -> '{}'", telefonoCrudo);
            return false;
        }
        return enviarTexto(texto, chatId);
    }

    /**
     * Normaliza un telefono boliviano al formato de chatId de WhatsApp.
     *
     * <p>Reglas:
     * <ul>
     *   <li>null / vacio -&gt; null</li>
     *   <li>si ya termina en {@code @c.us} o {@code @g.us} -&gt; se devuelve tal cual</li>
     *   <li>se descartan +, espacios, guiones y parentesis (queda solo digitos)</li>
     *   <li>8 digitos que empiezan con 6 o 7 (celular boliviano) -&gt; se antepone el prefijo de pais</li>
     *   <li>largo prefijo + 8 digitos (11 con "591") arrancando con el prefijo -&gt; se usa tal cual</li>
     *   <li>cualquier otro largo -&gt; null (se loguea un warn)</li>
     * </ul>
     *
     * @param telefonoCrudo telefono en cualquier formato
     * @return chatId con formato {@code 591XXXXXXXX@c.us}, o null si es invalido
     */
    public static String normalizarChatId(String telefonoCrudo) {
        if (telefonoCrudo == null) {
            return null;
        }

        String valor = telefonoCrudo.trim();
        if (valor.isEmpty()) {
            return null;
        }

        // Ya viene como chatId individual: se devuelve normalizado en minúsculas, porque
        // openWA rechaza el sufijo en mayúsculas ('78888274@C.US').
        String minusculas = valor.toLowerCase();
        if (minusculas.endsWith(SUFIJO_CONTACTO)) {
            return minusculas;
        }

        // Un id de GRUPO no puede salir de acá. Este método arma el chat individual de un
        // cliente, y su resultado alimenta mensajes redactados para el cliente ("le
        // confirmamos que su pedido ya fue entregado"). Si un resolver mal configurado
        // devolviera el valor de openwa.groups, ese texto terminaría publicado en el grupo
        // interno. Se corta acá, no en el llamador.
        if (minusculas.endsWith(SUFIJO_GRUPO)) {
            logger.warn("Se intentó usar un id de GRUPO como destinatario individual, se descarta: '{}'. "
                      + "Para enviar a un grupo usá enviarNotificacionAGrupos().", telefonoCrudo);
            return null;
        }

        String soloDigitos = soloDigitos(valor);
        if (soloDigitos.isEmpty()) {
            logger.warn("Telefono sin digitos, no se puede normalizar: '{}'", telefonoCrudo);
            return null;
        }

        String prefijo = prefijoPaisEstatico;
        String numeroCompleto;

        if (soloDigitos.length() == LARGO_CELULAR_BO
                && (soloDigitos.charAt(0) == '6' || soloDigitos.charAt(0) == '7')) {
            // Celular boliviano sin codigo de pais.
            numeroCompleto = prefijo + soloDigitos;
        } else if (soloDigitos.length() == prefijo.length() + LARGO_CELULAR_BO
                && soloDigitos.startsWith(prefijo)
                && (soloDigitos.charAt(prefijo.length()) == '6'
                 || soloDigitos.charAt(prefijo.length()) == '7')) {
            // Ya trae el codigo de pais. Se exige 6/7 igual que en la rama de 8 digitos: sin
            // esa comprobacion, un fijo con prefijo ('591 2 2456789') pasaba el filtro y se
            // convertia en un chatId inexistente, con lo que el fallo aparecia recien como
            // un no-2xx de openWA en vez de un warn claro de telefono invalido.
            numeroCompleto = soloDigitos;
        } else {
            // Segundo intento: el campo trae varios números o basura alrededor. En SAP estos
            // campos son texto libre y hay 69 clientes así: '"71551608...',
            // '72044730 - 72045404', '6663852 - 72985500'. Se parte por los separadores y se
            // toma el PRIMER token que sea un celular boliviano completo.
            //
            // Se hace en este orden y no al revés a propósito: '708 14 432' es UN número
            // escrito con espacios, y solo la pasada anterior (juntar todos los dígitos) lo
            // recupera. Tokenizar primero lo rompería en tres pedazos inútiles. Y al revés,
            // pegar los dígitos de '6663852 - 72985500' daría '666385272985500', del que un
            // barrido ingenuo sacaría '66638527' — un número que no es de nadie. Por eso el
            // token tiene que valer por sí solo, sin cruzar el separador.
            String candidato = primerCelularEnTokens(valor);
            if (candidato == null) {
                logger.warn("Telefono con formato no reconocido ({} digitos), se descarta: '{}'",
                        soloDigitos.length(), telefonoCrudo);
                return null;
            }
            logger.debug("Telefono recuperado de un campo con varios valores: '{}' -> '{}'",
                    telefonoCrudo, candidato);
            numeroCompleto = prefijo + candidato;
        }

        return numeroCompleto + SUFIJO_CONTACTO;
    }

    // =====================================================
    // Internos
    // =====================================================

    /**
     * Envio real contra openWA. Traga toda excepcion y devuelve el resultado
     * para quien lo necesite ({@link #enviarMensajeACliente}); los metodos
     * historicos void simplemente lo ignoran.
     *
     * @return true si openWA respondio 2xx
     */
    private boolean enviarTexto(String texto, String chatId) {
        String url = openwaUrl + "/api/sessions/" + sessionId + "/messages/send-text";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("chatId", chatId);
        body.put("text", texto);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<Map<String, Object>>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Mensaje enviado correctamente a: {}", chatId);
                return true;
            }
            logger.warn("openWA respondio {} al enviar a {}", response.getStatusCode(), chatId);
            return false;
        } catch (Exception e) {
            logger.error("Error enviando mensaje a {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene maximo 2 grupos de la configuracion.
     */
    private List<String> obtenerGruposLimitados() {
        if (groupsConfig == null || groupsConfig.isEmpty()) {
            return new ArrayList<String>();
        }

        List<String> todos = Arrays.stream(groupsConfig.split(","))
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .collect(Collectors.toList());

        if (todos.size() > 2) {
            logger.warn("⚠️ openwa.groups tiene {} grupos; solo se usaran los 2 primeros", todos.size());
        }

        return todos.stream().limit(2).collect(Collectors.toList());
    }

    /**
     * Deja solo los digitos de una cadena (descarta +, espacios, guiones, parentesis).
     */
    private static String soloDigitos(String valor) {
        StringBuilder sb = new StringBuilder(valor.length());
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Busca el primer celular boliviano completo entre los grupos de dígitos del texto.
     *
     * <p>Corta en cada carácter que no sea dígito y evalúa cada grupo por separado. Que el
     * grupo tenga que medir exactamente {@value #LARGO_CELULAR_BO} y empezar en 6 o 7 es lo
     * que impide fabricar un número inexistente pegando el final de uno con el principio del
     * siguiente.
     *
     * @return los 8 dígitos sin código de país, o {@code null} si ningún grupo califica
     */
    private static String primerCelularEnTokens(String valor) {
        int i = 0;
        int largo = valor.length();
        while (i < largo) {
            char c = valor.charAt(i);
            if (c < '0' || c > '9') {
                i++;
                continue;
            }
            int inicio = i;
            while (i < largo && valor.charAt(i) >= '0' && valor.charAt(i) <= '9') {
                i++;
            }
            int largoToken = i - inicio;
            if (largoToken == LARGO_CELULAR_BO) {
                char primero = valor.charAt(inicio);
                if (primero == '6' || primero == '7') {
                    return valor.substring(inicio, i);
                }
            }
        }
        return null;
    }
}
