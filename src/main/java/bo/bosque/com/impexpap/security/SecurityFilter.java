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

/**
 * Rechaza herramientas de escaneo por su User-Agent.
 *
 * <h3>Lo que este filtro dejo de hacer, y por que</h3>
 * Antes tambien evaluaba el contenido de los parametros contra tres expresiones
 * regulares que pretendian detectar SQL injection. Se sacaron:
 *
 * <ol>
 *   <li>{@code .*['"].*} rechazaba <b>cualquier valor con una comilla o un
 *       apostrofo</b>. O sea que un apellido como {@code D'Angelo} o una glosa
 *       con comillas daba 403, sin mensaje que lo explicara.</li>
 *   <li>{@code \b(union|select|from|where|drop|--)\b} rechaza palabras que
 *       aparecen en texto libre en espanol y en ingles ({@code from},
 *       {@code where}). Ademas {@code --} estaba repetido en la alternancia.</li>
 *   <li>Sobre todo: <b>no aportaba defensa real.</b> En este proyecto no se
 *       concatena SQL — el test {@code SinSqlCrudoTest} lo hace cumplir, los
 *       nombres de procedimiento son literales y todos los valores viajan como
 *       parametros de {@code PreparedStatement}. Filtrar comillas a la entrada
 *       es la defensa que se usa cuando NO hay parametros vinculados. Aca los
 *       hay, y el filtro solo agregaba falsos positivos y una falsa sensacion de
 *       seguridad.</li>
 *   <li>Y tenia un efecto lateral silencioso: {@code request.getParameterMap()}
 *       sobre un POST {@code application/x-www-form-urlencoded} hace que Tomcat
 *       <b>consuma el body</b>, con lo cual el {@code @RequestBody} del
 *       controlador llegaba vacio. Esta documentado en
 *       {@code WhatsAppWebhookController}, que se tuvo que escribir esquivandolo.</li>
 * </ol>
 *
 * <p>Queda la lista de User-Agents, que es barata, no tiene falsos positivos
 * sobre trafico legitimo y sirve para sacarse de encima el ruido de fondo de los
 * escaneres automaticos. No es un WAF y no pretende serlo.
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    // Lista de User-Agents sospechosos
    private final List<String> suspiciousUserAgents = Arrays.asList(
            "sqlmap", "nikto", "nessus", "nmap", "burpsuite", "ZAP", "masscan", "python-requests"
    );

    private final ClienteIp clienteIp;

    public SecurityFilter(ClienteIp clienteIp) {
        this.clienteIp = clienteIp;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String userAgent = request.getHeader("User-Agent");

        // Verificar User-Agent sospechoso
        if (userAgent != null && suspiciousUserAgents.stream().anyMatch(userAgent::contains)) {
            logger.warn("Suspicious User-Agent detected from IP {}: {}", clienteIp.de(request), userAgent);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\": \"Acceso denegado\", \"ok\": false}");
            return;
        }

        chain.doFilter(request, response);
    }
}
