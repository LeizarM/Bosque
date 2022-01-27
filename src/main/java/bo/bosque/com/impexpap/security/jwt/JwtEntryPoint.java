package bo.bosque.com.impexpap.security.jwt;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private final static Logger logger = LoggerFactory.getLogger(JwtEntryPoint.class);

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e) throws IOException {
        logger.error("Fail en el método commence");
        res.setContentType("application/json");
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        logger.error(e.getMessage());
        res.getOutputStream().println("{ \"error\": error, \"ok\": false }");
        
    }
}