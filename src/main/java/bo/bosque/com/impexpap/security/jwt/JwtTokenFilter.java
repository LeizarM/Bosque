package bo.bosque.com.impexpap.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.security.ClienteIp;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtTokenFilter extends OncePerRequestFilter {

    private final static Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);

    private static final String PREFIJO_BEARER = "Bearer ";

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ILoginDao ldao;

    @Autowired
    private JwtEntryPoint jwtEntryPoint;

    @Autowired
    private ClienteIp clienteIp;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getToken(req);
            if (token != null && jwtProvider.validateToken(token)) {
                String nombreUsuario = jwtProvider.getNombreUsuarioFromToken(token);
                UserDetails loginDetails = ldao.loadUserByUsername(nombreUsuario);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(loginDetails, null, loginDetails.getAuthorities());
                // La identidad firmada (codUsuario, codEmpleado, codEmpresa) baja al
                // SecurityContext. Es lo que permite que los controladores dejen de
                // recibir audUsuario y codUsuario por el body. Ver DatosToken.
                auth.setDetails(jwtProvider.getDatosFromToken(token));
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Resetear contador de fallos para esta IP cuando la autenticación es exitosa
                jwtEntryPoint.resetFailureCount(clienteIp.de(req));
            }
        } catch (Exception e) {
            logger.error("fail en el método doFilter " + e.getMessage());
        }
        filterChain.doFilter(req, res);
    }

    /**
     * El token del header {@code Authorization}.
     *
     * <p>Se corta por posicion y no con {@code replace("Bearer ", "")}: el replace
     * borra TODAS las apariciones, asi que un token que contuviera esa subcadena
     * quedaba mutilado. Ademas se exige el espacio, para que un {@code "Bearer"}
     * pelado no pase el {@code startsWith} y llegue sin limpiar.
     */
    private String getToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIJO_BEARER)) {
            return null;
        }
        String token = header.substring(PREFIJO_BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
