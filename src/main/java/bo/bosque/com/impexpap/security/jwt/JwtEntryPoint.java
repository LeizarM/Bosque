package bo.bosque.com.impexpap.security.jwt;


import bo.bosque.com.impexpap.security.ClienteIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private final static Logger logger = LoggerFactory.getLogger(JwtEntryPoint.class);

    /**
     * Cuanto se conserva el contador de fallos de una IP que dejo de aparecer.
     * Es lo que permite limpiarlo: sin marca de tiempo no habia forma de saber
     * cual borrar, y por eso {@code failureCounters} no se limpiaba NUNCA.
     */
    private static final long TTL_CONTADOR_MS = 60L * 60L * 1000L;   // 1 hora

    /** Intentos fallidos por IP, con el instante del ultimo. */
    private final Map<String, Fallos> failureCounters = new ConcurrentHashMap<>();

    // Mapa para almacenar tiempos de bloqueo
    private final Map<String, Long> blockUntilTime = new ConcurrentHashMap<>();

    // Umbrales para bloqueo progresivo
    private final int THRESHOLD_1 = 3;  // 3 fallos: esperar 1 minuto
    private final int THRESHOLD_2 = 5;  // 5 fallos: esperar 5 minutos
    private final int THRESHOLD_3 = 10; // 10 fallos: esperar 30 minutos

    private final ClienteIp clienteIp;

    public JwtEntryPoint(ClienteIp clienteIp) {
        this.clienteIp = clienteIp;
    }

    /** Contador con marca de tiempo, para poder caducarlo. */
    private static final class Fallos {
        final int cantidad;
        final long ultimo;
        Fallos(int cantidad, long ultimo) {
            this.cantidad = cantidad;
            this.ultimo   = ultimo;
        }
    }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e) throws IOException {
        String ip = clienteIp.de(req);

        // Verificar si la IP está bloqueada actualmente
        Long blockedUntil = blockUntilTime.get(ip);
        if (blockedUntil != null && blockedUntil > System.currentTimeMillis()) {
            long waitSeconds = (blockedUntil - System.currentTimeMillis()) / 1000;
            logger.warn("Blocked authentication attempt from IP: {}", ip);
            res.setContentType("application/json");
            // 429 y no 401: el pedido no es que las credenciales esten mal, es que
            // hay que esperar. Antes mandaba 401 con un comentario que decia "429".
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(waitSeconds));
            res.getWriter().write("{\"error\": \"Demasiados intentos fallidos. Intente nuevamente en "
                    + waitSeconds + " segundos\", \"ok\": false}");
            return;
        }

        // Incrementar contador de fallos
        long ahora = System.currentTimeMillis();
        Fallos previos = failureCounters.get(ip);
        int failCount = (previos == null ? 0 : previos.cantidad) + 1;
        failureCounters.put(ip, new Fallos(failCount, ahora));

        // Aplicar bloqueo progresivo según el número de fallos
        if (failCount >= THRESHOLD_3) {
            // Bloquear por 30 minutos
            blockUntilTime.put(ip, ahora + (30 * 60 * 1000));
            res.setHeader("Retry-After", String.valueOf(30 * 60));
            logger.warn("IP {} blocked for 30 minutes after {} failed attempts", ip, failCount);
        } else if (failCount >= THRESHOLD_2) {
            // Bloquear por 5 minutos
            blockUntilTime.put(ip, ahora + (5 * 60 * 1000));
            res.setHeader("Retry-After", String.valueOf(5 * 60));
            logger.warn("IP {} blocked for 5 minutes after {} failed attempts", ip, failCount);
        } else if (failCount >= THRESHOLD_1) {
            // Bloquear por 1 minuto
            blockUntilTime.put(ip, ahora + (60 * 1000));
            res.setHeader("Retry-After", String.valueOf(60));
            logger.warn("IP {} blocked for 1 minute after {} failed attempts", ip, failCount);
        }

        // Respuesta normal para error de autenticación
        logger.error("Authentication failure: {}", e.getMessage());
        res.setContentType("application/json");
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.getWriter().write("{\"error\": \"No autorizado\", \"ok\": false}");
    }

    /**
     * Método para resetear el contador de fallos cuando la autenticación es exitosa
     * @param ip La dirección IP del cliente
     */
    public void resetFailureCount(String ip) {
        failureCounters.remove(ip);
        blockUntilTime.remove(ip);
    }

    /**
     * Limpieza periódica de los mapas de bloqueo.
     *
     * <p>Ahora limpia LOS DOS. {@code failureCounters} no se vaciaba nunca, y su
     * clave es una IP: cada IP que fallara una vez dejaba una entrada para
     * siempre. Con la resolucion de IP arreglada ({@code ClienteIp}) el atacante
     * ya no elige la clave, pero el mapa igual crecia con el trafico normal.
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void cleanup() {
        long now = System.currentTimeMillis();
        blockUntilTime.entrySet().removeIf(entry -> entry.getValue() < now);
        failureCounters.entrySet().removeIf(entry -> now - entry.getValue().ultimo > TTL_CONTADOR_MS);
        logger.info("JwtEntryPoint block list cleaned up ({} bloqueos, {} contadores vigentes)",
                blockUntilTime.size(), failureCounters.size());
    }
}
