package bo.bosque.com.impexpap.security.jwt;


import io.github.bucket4j.Bucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private final static Logger logger = LoggerFactory.getLogger(JwtEntryPoint.class);

    // Cache para almacenar los buckets por IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Mapa para rastrear intentos fallidos consecutivos por IP
    private final Map<String, Integer> failureCounters = new ConcurrentHashMap<>();

    // Mapa para almacenar tiempos de bloqueo
    private final Map<String, Long> blockUntilTime = new ConcurrentHashMap<>();

    // Umbrales para bloqueo progresivo
    private final int THRESHOLD_1 = 3;  // 3 fallos: esperar 1 minuto
    private final int THRESHOLD_2 = 5;  // 5 fallos: esperar 5 minutos
    private final int THRESHOLD_3 = 10; // 10 fallos: esperar 30 minutos

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e) throws IOException {
        String ip = getClientIP(req);

        // Verificar si la IP está bloqueada actualmente
        Long blockedUntil = blockUntilTime.get(ip);
        if (blockedUntil != null && blockedUntil > System.currentTimeMillis()) {
            long waitSeconds = (blockedUntil - System.currentTimeMillis()) / 1000;
            logger.warn("Blocked authentication attempt from IP: {}", ip);
            res.setContentType("application/json");
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 429
            res.setHeader("Retry-After", String.valueOf(waitSeconds));
            res.getWriter().write("{\"error\": \"Demasiados intentos fallidos. Intente nuevamente en "
                    + waitSeconds + " segundos\", \"ok\": false}");
            return;
        }

        // Incrementar contador de fallos
        int failCount = failureCounters.getOrDefault(ip, 0) + 1;
        failureCounters.put(ip, failCount);

        // Aplicar bloqueo progresivo según el número de fallos
        if (failCount >= THRESHOLD_3) {
            // Bloquear por 30 minutos
            blockUntilTime.put(ip, System.currentTimeMillis() + (30 * 60 * 1000));
            res.setHeader("Retry-After", String.valueOf(30 * 60));
            logger.warn("IP {} blocked for 30 minutes after {} failed attempts", ip, failCount);
        } else if (failCount >= THRESHOLD_2) {
            // Bloquear por 5 minutos
            blockUntilTime.put(ip, System.currentTimeMillis() + (5 * 60 * 1000));
            res.setHeader("Retry-After", String.valueOf(5 * 60));
            logger.warn("IP {} blocked for 5 minutes after {} failed attempts", ip, failCount);
        } else if (failCount >= THRESHOLD_1) {
            // Bloquear por 1 minuto
            blockUntilTime.put(ip, System.currentTimeMillis() + (60 * 1000));
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
     * Limpieza periódica de los mapas de bloqueo
     */
    @Scheduled(fixedRate = 3600000) // Cada hora
    public void cleanup() {
        long now = System.currentTimeMillis();
        blockUntilTime.entrySet().removeIf(entry -> entry.getValue() < now);
        logger.info("JwtEntryPoint block list cleaned up");
    }

    /**
     * Obtiene la dirección IP real del cliente
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}