package bo.bosque.com.impexpap.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WhatsAppService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    // =====================================================
    // CONFIGURACIÓN OPENWA - PRODUCCIÓN (Podman + Docker)
    // =====================================================

    // Opción 1 (RECOMENDADA): Usar la IP del servidor
    //private final String openwaUrl = "http://181.114.119.195:2785";

    // Opción 2: Usar host.containers.internal (descomentar si la IP falla)
    private final String openwaUrl = "http://host.containers.internal:2785";

    private final String sessionId    = "58812227-7ca1-4205-b5c2-7dbc01870fc9";
    private final String apiKey       = "dev-admin-key";
    private final String defaultPhone = "59178888274@c.us";
    private final String groupsConfig = "59178888272-1422893975@g.us";   // Máximo 2 grupos

    /**
     * Envía mensaje de texto a un número o grupo
     */
    public void enviarMensaje(String texto, String chatId) {
        String url = openwaUrl + "/api/sessions/" + sessionId + "/messages/send-text";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("chatId", chatId);
        body.put("text", texto);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Mensaje enviado correctamente a: {}", chatId);
            }
        } catch (Exception e) {
            logger.error("Error enviando mensaje a {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Envía notificación con formato a los grupos configurados (máximo 2)
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
     * Obtiene máximo 2 grupos de la configuración
     */
    private List<String> obtenerGruposLimitados() {
        if (groupsConfig == null || groupsConfig.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(groupsConfig.split(","))
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .limit(2)
                .collect(Collectors.toList());
    }

    /**
     * Método para pruebas (envía a tu número personal)
     */
    public void enviarPrueba(String mensaje) {
        String texto = "🧪 [PRUEBA] " + mensaje;
        enviarMensaje(texto, defaultPhone);
    }
}