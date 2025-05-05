package bo.bosque.com.impexpap.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Configuración del límite: 5 solicitudes por minuto por IP
    private final int CAPACITY = 5;
    private final int REFILL_TOKENS = 5;
    private final int REFILL_MINUTES = 1;

    // Lista de rutas protegidas
    private final List<String> protectedPaths = Arrays.asList(
            "/auth/login",
            "/api/sensitive-endpoint",
            "/api/another-protected-path"
            // Añade aquí todas las rutas que quieras proteger
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Verificar si la ruta actual está en la lista de rutas protegidas
        boolean isProtectedPath = protectedPaths.stream()
                .anyMatch(requestPath::contains);

        if (isProtectedPath) {
            String ip = getClientIP(request);
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
        return buckets.computeIfAbsent(ip, key -> {
            // Crear un nuevo bucket para esta IP
            Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.greedy(REFILL_TOKENS, Duration.ofMinutes(REFILL_MINUTES)));
            return Bucket.builder().addLimit(limit).build();
        });
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}