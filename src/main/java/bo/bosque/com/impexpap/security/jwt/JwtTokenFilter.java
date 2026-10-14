package bo.bosque.com.impexpap.security.jwt;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import bo.bosque.com.impexpap.commons.SesionService;
import bo.bosque.com.impexpap.controller.LoginController;
import bo.bosque.com.impexpap.dao.ILoginDao;
import bo.bosque.com.impexpap.security.ClienteIp;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class JwtTokenFilter extends OncePerRequestFilter {

    private final static Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);

    private static final String PREFIJO_BEARER = "Bearer ";

    /** Código del 401 de sesión cerrada; la app lo usa para mostrar el motivo en el login. */
    static final String SESION_CERRADA = "SESION_CERRADA";

    /** Un bloqueo de administrador no se levanta por correo: no se remite a «¿Olvidaste tu contraseña?». */
    static final String MSG_BLOQUEADO_ADMIN = "Un administrador bloqueó tu usuario. Comunícate con un administrador.";

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ILoginDao ldao;

    @Autowired
    private JwtEntryPoint jwtEntryPoint;

    @Autowired
    private ClienteIp clienteIp;

    @Autowired
    private SesionService sesiones;

    /**
     * El login y la recuperación no usan el token. Sin esto, un cliente que lo mandara de más recibía el 401
     * de sesión cerrada justo en la pantalla a la que remite el mensaje de bloqueo.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String ruta = req.getRequestURI();
        return ruta.contains("/auth/login") || ruta.contains("/auth/recuperacion");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
        String sesionCerrada = null;
        try {
            String token = getToken(req);
            // La firma se verifica una vez; el usuario y su sesión salen de una sola consulta.
            Claims claims = token != null ? jwtProvider.leer(token) : null;
            if (claims != null) {
                String nombreUsuario = claims.getSubject();
                DatosToken datos = jwtProvider.datosDe(claims);
                UsuarioToken loginDetails = ldao.cargarParaToken(nombreUsuario, datos.getIdSesion());
                sesionCerrada = motivoDeCierre(loginDetails);

                if (sesionCerrada != null) {
                    // Se cierran todas sus sesiones: así desbloquearlo después no revive los tokens que ya tenía.
                    sesiones.cerrarTodas(datos.getCodUsuario(),
                            loginDetails.isEnabled() ? SesionService.MOTIVO_BLOQUEO : SesionService.MOTIVO_BAJA, null);
                } else if (loginDetails.isSesionCerrada()) {
                    sesionCerrada = SesionService.mensajeDeCierre(loginDetails.getMotivoCierreSesion());
                }

                if (sesionCerrada == null) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(loginDetails, null, loginDetails.getAuthorities());
                    // La identidad firmada (codUsuario, codEmpleado, codEmpresa) baja al
                    // SecurityContext. Es lo que permite que los controladores dejen de
                    // recibir audUsuario y codUsuario por el body. Ver DatosToken.
                    auth.setDetails(datos);
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // Resetear contador de fallos para esta IP cuando la autenticación es exitosa
                    jwtEntryPoint.resetFailureCount(clienteIp.de(req));
                } else {
                    logger.warn("Sesión cerrada de '{}' desde {}: {}", nombreUsuario, clienteIp.de(req), sesionCerrada);
                }
            }
        } catch (Exception e) {
            logger.error("fail en el método doFilter " + e.getMessage());
        }
        if (sesionCerrada != null) {
            // Se responde aquí y no en JwtEntryPoint, que contaría el pedido como fallo de la IP.
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write(JSON.writeValueAsString(cuerpoSesionCerrada(sesionCerrada)));
            return;
        }
        filterChain.doFilter(req, res);
    }

    /**
     * El token sigue vigente, pero el usuario ya no puede entrar: dado de baja o bloqueado por un administrador
     * (un bloqueo por intentos no cuenta: si no, tres claves falsas de un tercero sacarían a cualquiera).
     */
    static String motivoDeCierre(UserDetails usuario) {
        if (!usuario.isEnabled()) return LoginController.MSG_INACTIVO;
        if (!usuario.isAccountNonLocked()) return MSG_BLOQUEADO_ADMIN;
        return null;
    }

    private static Map<String, Object> cuerpoSesionCerrada(String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("ok", false);
        cuerpo.put("error", SESION_CERRADA);
        cuerpo.put("mensaje", mensaje);
        return cuerpo;
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
