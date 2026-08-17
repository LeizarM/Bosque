package bo.bosque.com.impexpap.security.jwt;

import java.util.Base64;
import java.util.Date;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import bo.bosque.com.impexpap.model.Login;


/**
 * Firma y verificacion de los JWT.
 *
 * <h3>De donde sale la clave</h3>
 * De {@code jwt.secret}, que a su vez sale de la variable de entorno
 * {@code JWT_SECRET}. <b>No hay default</b>: si falta, la aplicacion no arranca.
 *
 * <p>Antes la clave era una constante {@code RSA_PRIVATE} escrita dentro de
 * {@code JwtConfig.java} y versionada en git — y encima usada como secreto
 * SIMETRICO de HS512, o sea que el texto del PEM ERA la clave HMAC. Cualquiera
 * con acceso al repositorio, al jar, o a la copia que habia en {@code bin/},
 * podia firmarse un token con el {@code login}, el {@code codUsuario} y el rol
 * que quisiera. Esa clase ya no existe.
 *
 * <p>El secreto se valida al construir el bean y no en el primer login: un
 * secreto corto o mal codificado tiene que romper el arranque, no aparecer
 * recien cuando alguien intenta entrar.
 *
 * <h3>Los claims son parte del contrato, no adorno</h3>
 * {@code codUsuario} viaja como {@code jti} y {@code codEmpresa} como claim.
 * {@link JwtTokenFilter} los baja a {@link DatosToken} y de ahi los leen los
 * controladores. Es lo que permite dejar de recibir {@code audUsuario} y
 * {@code codUsuario} por el body — donde el cliente los elegia a gusto.
 */
@Component
public class JwtProvider {
    private final static Logger logger = LoggerFactory.getLogger(JwtProvider.class);

    /** Largo minimo del secreto ya decodificado. HS512 usa bloques de 512 bits. */
    private static final int MINIMO_BYTES_SECRETO = 64;

    /** Piso de {@code jwt.expiration}, en SEGUNDOS. Menos de un minuto no es usable. */
    private static final int MINIMO_EXPIRACION_SEG = 60;

    /** Techo de {@code jwt.expiration}, en SEGUNDOS: 7 dias. Ver {@link #validarExpiracion}. */
    private static final int MAXIMO_EXPIRACION_SEG = 7 * 24 * 3600;

    private final byte[] claveFirma;
    private final int expiration;

    public JwtProvider(@Value("${jwt.secret}") String secretoBase64,
                       @Value("${jwt.expiration}") int expiration) {
        this.claveFirma = decodificarSecreto(secretoBase64);
        this.expiration = validarExpiracion(expiration);
    }

    /**
     * {@code jwt.expiration} esta en SEGUNDOS, y el rango se valida al arrancar.
     *
     * <h3>El bug que esto destapa</h3>
     * Produccion tenia {@code jwt.expiration=86400000} — alguien lo escribio en
     * milisegundos. Como el calculo era {@code expiration * 1000} entre dos
     * {@code int}, el resultado (86.400.000.000) <b>desbordaba</b> y quedaba en
     * 500.654.080 ms: tokens de 5,8 dias. Un numero que nadie eligio, producto
     * de un desborde silencioso.
     *
     * <p>Al corregir la aritmetica a {@code 1000L} el desborde desaparece y ese
     * mismo valor pasa a significar <b>1000 dias</b> de token vivo. O sea que
     * arreglar la multiplicacion, solo, empeora las cosas.
     *
     * <p>Por eso el rango: un valor en milisegundos cae fuera del techo y la
     * aplicacion <b>no arranca</b>, con un mensaje que dice exactamente que
     * poner. Un token que dura tres anios no se nota hasta que hay que revocar
     * uno y no se puede.
     */
    private static int validarExpiracion(int expiration) {
        if (expiration < MINIMO_EXPIRACION_SEG || expiration > MAXIMO_EXPIRACION_SEG) {
            throw new IllegalStateException(
                    "jwt.expiration=" + expiration + " esta fuera de rango. Se expresa en "
                  + "SEGUNDOS y tiene que estar entre " + MINIMO_EXPIRACION_SEG + " y "
                  + MAXIMO_EXPIRACION_SEG + " (7 dias). Valores utiles: 36000 = 10 horas, "
                  + "86400 = 1 dia. Si el numero parece milisegundos (86400000), ese es el "
                  + "error: dividilo por 1000.");
        }
        return expiration;
    }

    /**
     * Pasa el secreto de Base64 a bytes y se planta si no sirve.
     *
     * <p>Se exige Base64 y no texto plano para que el secreto pueda tener bytes
     * arbitrarios sin pelearse con el parseo de {@code .properties} ni con el
     * shell que exporta la variable.
     */
    private static byte[] decodificarSecreto(String secretoBase64) {
        if (secretoBase64 == null || secretoBase64.trim().isEmpty()) {
            throw new IllegalStateException(
                    "jwt.secret esta vacio. Definir la variable de entorno JWT_SECRET. "
                  + "Generar con: openssl rand -base64 64");
        }
        byte[] clave;
        try {
            clave = Base64.getDecoder().decode(secretoBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "jwt.secret no es Base64 valido. Generar con: openssl rand -base64 64", e);
        }
        if (clave.length < MINIMO_BYTES_SECRETO) {
            throw new IllegalStateException(
                    "jwt.secret tiene " + clave.length + " bytes y HS512 necesita al menos "
                  + MINIMO_BYTES_SECRETO + ". Generar con: openssl rand -base64 64");
        }
        return clave;
    }

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
                .setExpiration(new Date(new Date().getTime() + expiration * 1000L))
                .signWith(SignatureAlgorithm.HS512, claveFirma)
                .compact();
    }

    /**
     * Procedimiento para obtener el token enviado por el usuario
     * @param token
     * @return
     */
    public String getNombreUsuarioFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * La identidad que viaja firmada dentro del token.
     *
     * <p>Se llama una sola vez por request, desde {@link JwtTokenFilter}: parsear
     * el token es barato pero no gratis, y todos los que lo necesitan lo leen
     * despues del {@code Authentication}.
     */
    public DatosToken getDatosFromToken(String token) {
        Claims c = parseClaims(token);
        return new DatosToken(
                aEntero(c.getId()),
                aEntero(c.get("codEmpleado")),
                aEntero(c.get("codEmpresa")),
                c.get("tipoUsuario", String.class));
    }

    /**
     * Procedimiento para validar el token
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
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

    private Claims parseClaims(String token) {
        return Jwts.parser().setSigningKey(claveFirma).parseClaimsJws(token).getBody();
    }

    /**
     * Los numericos de un JWT llegan como {@code Integer}, como {@code Long} o
     * como texto, segun quien lo serializo. Cualquiera de las tres tiene que dar
     * el mismo entero; lo que no se entiende vale 0, que aguas abajo se trata
     * como "no identificado" y termina en 403.
     */
    private static int aEntero(Object valor) {
        if (valor == null) return 0;
        if (valor instanceof Number) return ((Number) valor).intValue();
        try {
            return Integer.parseInt(valor.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
