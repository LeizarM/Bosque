package bo.bosque.com.impexpap.security.jwt;
import java.util.Date;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import bo.bosque.com.impexpap.model.Login;


@Component
public class JwtProvider {
    private final static Logger logger = LoggerFactory.getLogger(JwtProvider.class);



    @Value("${jwt.expiration}")
    private int expiration;

    /**
     * Procedimiento para generar el Token para el Login
     * @param authentication
     * @return
     */
    public String generateToken(Authentication authentication, Login login) {

        login.setLogin( authentication.getName() );
        login.setAuthorities(authentication.getAuthorities() );

        return Jwts.builder().setSubject( login.getLogin() )
                .setId( String.valueOf ( login.getCodUsuario() ) )
                .claim ("nombreCompleto", login.getEmpleado().getPersona().getDatoPersona() )
                .claim("codEmpleado", login.getCodEmpleado())
                .claim("cargo", login.getEmpleado().getEmpleadoCargo().getCargoSucursal().getCargo().getDescripcion() )
                .claim("codSucursal", login.getCodSucursal())
                .claim( "codEmpresa", login.getCodEmpresa())
                .claim( "codCiudad", login.getCodCiudad())
                .claim( "tipoUsuario", login.getTipoUsuario())
                .claim("versionApp", login.getVersionApp())
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + expiration * 1000))
                .signWith(SignatureAlgorithm.HS512, JwtConfig.RSA_PRIVATE)
                .compact();
    }

    /**
     * Procedimiento para obtener el token enviado por el usuario
     * @param token
     * @return
     */
    public String getNombreUsuarioFromToken(String token) {
        return Jwts.parser().setSigningKey(JwtConfig.RSA_PRIVATE).parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * Procedimiento para validar el token
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(JwtConfig.RSA_PRIVATE)
                          .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("token mal formado");
        } catch (UnsupportedJwtException e) {
            logger.error("token no soportado");
        } catch (ExpiredJwtException e) {
            logger.error("token expirado");
        } catch (IllegalArgumentException e) {
            logger.error("token vacío");
        } catch (SignatureException e) {
            logger.error("fail en la firma");
        }
        return false;
    }
}