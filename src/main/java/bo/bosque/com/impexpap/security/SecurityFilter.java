package bo.bosque.com.impexpap.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    // Lista de User-Agents sospechosos
    private final List<String> suspiciousUserAgents = Arrays.asList(
            "sqlmap", "nikto", "nessus", "nmap", "burpsuite", "ZAP", "masscan", "python-requests"
    );

    // Lista de patrones de ataque comunes
    private final List<Pattern> attackPatterns = Arrays.asList(
            Pattern.compile(".*\\b(union|select|from|where|drop|--|--)\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*['\"].*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\b(or|and)\\s+\\d+=\\d+.*", Pattern.CASE_INSENSITIVE)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String userAgent = request.getHeader("User-Agent");
        String ip = getClientIP(request);

        // Verificar User-Agent sospechoso
        if (userAgent != null && suspiciousUserAgents.stream().anyMatch(userAgent::contains)) {
            logger.warn("Suspicious User-Agent detected from IP {}: {}", ip, userAgent);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Acceso denegado\", \"ok\": false}");
            return;
        }

        // Determinar si es una solicitud de carga de archivos
        String contentType = request.getContentType();
        boolean isMultipart = contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");

        // Para subidas de archivos, omitir la verificación de parámetros del cuerpo
        if (!isMultipart) {
            // Verificar patrones de ataque solo en peticiones que no son de carga de archivos
            Map<String, String[]> params = request.getParameterMap();
            for (String[] values : params.values()) {
                for (String value : values) {
                    for (Pattern pattern : attackPatterns) {
                        if (pattern.matcher(value).matches()) {
                            logger.warn("Possible attack pattern detected from IP {}: {}", ip, value);
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"error\": \"Acceso denegado\", \"ok\": false}");
                            return;
                        }
                    }
                }
            }
        } else {
            // Es una petición multipart (subida de archivos), permitir sin revisar el contenido del archivo
            logger.debug("Multipart request detected, allowing file upload from IP {}", ip);
        }

        // Verificar solo parámetros de la URL para todas las peticiones
        String queryString = request.getQueryString();
        if (queryString != null) {
            for (Pattern pattern : attackPatterns) {
                if (pattern.matcher(queryString).matches()) {
                    logger.warn("Possible attack pattern in URL from IP {}: {}", ip, queryString);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\": \"Acceso denegado\", \"ok\": false}");
                    return;
                }
            }
        }

        // Si llegamos aquí, todo está bien, continuar con la cadena de filtros
        chain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}