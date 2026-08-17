package bo.bosque.com.impexpap.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    // Cache para almacenar los buckets por IP
    private final Map<String, Registro> buckets = new ConcurrentHashMap<>();

    // Configuración del límite: 5 solicitudes por minuto por IP
    private final int CAPACITY = 5;
    private final int REFILL_TOKENS = 5;
    private final int REFILL_MINUTES = 1;

    /**
     * Cuanto se guarda el bucket de una IP que dejo de aparecer. Con el bucket
     * lleno de vuelta, conservarlo no aporta nada y el mapa solo crecia.
     */
    private static final long TTL_BUCKET_MS = 30L * 60L * 1000L;   // 30 minutos

    // Lista de rutas protegidas
    private final List<String> protectedPaths = Arrays.asList(
            "/auth/login",
            "/api/sensitive-endpoint",
            "/api/another-protected-path"
            // Añade aquí todas las rutas que quieras proteger
    );

    private final ClienteIp clienteIp;

    public RateLimitFilter(ClienteIp clienteIp) {
        this.clienteIp = clienteIp;
    }

    /** Bucket con la marca del ultimo uso, para poder caducarlo. */
    private static final class Registro {
        final Bucket bucket;
        volatile long ultimoUso;
        Registro(Bucket bucket, long ultimoUso) {
            this.bucket    = bucket;
            this.ultimoUso = ultimoUso;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Verificar si la ruta actual está en la lista de rutas protegidas
        boolean isProtectedPath = protectedPaths.stream()
                .anyMatch(requestPath::contains);

        if (isProtectedPath) {
            // La IP sale de ClienteIp y no de X-Forwarded-For a secas: esa cabecera la
            // escribe el cliente, y con un valor distinto por request este limite de
            // 5/minuto se evadia por completo.
            String ip = clienteIp.de(request);
            Bucket bucket = resolveBucket(ip);

            if (bucket.tryConsume(1)) {
                // Permitir la solicitud
                chain.doFilter(request, response);
            } else {
                // Rechazar la solicitud por exceder el límite
                log.warn("Rate limit exceeded for IP: {} on path: {}", ip, requestPath);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"mensaje\": \"Has excedido el límite de intentos. Por favor, intenta más tarde.\", \"status\": \"error\"}");
            }
        } else {
            // Para todas las demás solicitudes, no aplicar rate limiting
            chain.doFilter(request, response);
        }
    }

    private Bucket resolveBucket(String ip) {
        Registro registro = buckets.computeIfAbsent(ip, key -> {
            // Crear un nuevo bucket para esta IP
            Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.greedy(REFILL_TOKENS, Duration.ofMinutes(REFILL_MINUTES)));
            return new Registro(Bucket.builder().addLimit(limit).build(), System.currentTimeMillis());
        });
        registro.ultimoUso = System.currentTimeMillis();
        return registro.bucket;
    }

    /** Saca del mapa los buckets de IPs que ya no aparecen. Antes no se limpiaba nunca. */
    @Scheduled(fixedRate = 1800000) // Cada 30 minutos
    public void limpiarBucketsViejos() {
        long ahora = System.currentTimeMillis();
        int antes = buckets.size();
        buckets.entrySet().removeIf(e -> ahora - e.getValue().ultimoUso > TTL_BUCKET_MS);
        if (antes != buckets.size()) {
            log.debug("RateLimitFilter: {} buckets liberados, quedan {}", antes - buckets.size(), buckets.size());
        }
    }
}
