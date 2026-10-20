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
        return generateToken(authentication, login, 0);
    }

    /** {@code idSesion} (trec_sesion) viaja como claim {@code sid}; con 0 no va, como en los tokens anteriores a sql/07. */
    public String generateToken(Authentication authentication, Login login, long idSesion) {

        login.setLogin( authentication.getName() );
        login.setAuthorities(authentication.getAuthorities() );

        JwtBuilder token = Jwts.builder().setSubject( login.getLogin() )
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
                .setExpiration(new Date(new Date().getTime() + expiration * 1000L));
        if (idSesion > 0) token.claim("sid", idSesion);
        return token.signWith(SignatureAlgorithm.HS512, claveFirma).compact();
    }

    /** Lo que dura un token, en segundos (jwt.expiration ya validado). */
    public int getExpiracionSegundos() {
        return expiration;
    }

    // ── Postulantes externos del módulo de exámenes (tcpe_usuario) ─────────────────────────

    /** Claim que distingue el token del postulante. Los tokens de empleado no lo llevan. */
    public static final String CLAIM_TIPO = "tipo";
    public static final String TIPO_POSTULANTE = "POSTULANTE";

    /** Cuándo vencería un token emitido ahora (misma vida que el de empleado: jwt.expiration). */
    public Date expiracionDesdeAhora() {
        return new Date(System.currentTimeMillis() + expiration * 1000L);
    }

    /**
     * Token del postulante: {@code sub} = login de {@code tcpe_usuario}, {@code jti} = su {@code codUsuario}
     * (de {@code tcpe_usuario}, no de {@code tb_usuario}) y {@code tipo = POSTULANTE}. Sin {@code sid}:
     * el postulante no abre sesión en {@code trec_sesion}.
     */
    public String generarTokenPostulante(long codUsuario, String login, Date expira) {
        return Jwts.builder()
                .setSubject(login)
                .setId(String.valueOf(codUsuario))
                .claim(CLAIM_TIPO, TIPO_POSTULANTE)
                .setIssuedAt(new Date())
                .setExpiration(expira)
                .signWith(SignatureAlgorithm.HS512, claveFirma)
                .compact();
    }

    /** Si los claims (ya verificados) son de un postulante. */
    public static boolean esPostulante(Claims c) {
        return c != null && TIPO_POSTULANTE.equals(c.get(CLAIM_TIPO, String.class));
    }

    /** La identidad del postulante a partir de sus claims verificados. */
    public DatosPostulante datosPostulanteDe(Claims c) {
        return new DatosPostulante(aLargo(c.getId()), c.getSubject());
    }

    /**
     * Los claims de un token válido, o null si no lo es (mal formado, vencido, con otra firma o vacío).
     *
     * <p>{@link JwtTokenFilter} verifica la firma una sola vez por pedido y saca de aquí el login
     * ({@code getSubject()}) y la identidad ({@link #datosDe}).
     */
    public Claims leer(String token) {
        try {
            return parseClaims(token);
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
        return null;
    }

    /** La identidad que viaja firmada dentro del token, de los claims que devolvió {@link #leer}. */
    public DatosToken datosDe(Claims c) {
        return new DatosToken(
                aEntero(c.getId()),
                aEntero(c.get("codEmpleado")),
                aEntero(c.get("codEmpresa")),
                c.get("tipoUsuario", String.class),
                c.get("nombreCompleto", String.class),
                aLargo(c.get("sid")));
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

    /** Igual que {@link #aEntero} pero para el {@code sid}, que es BIGINT en la base. */
    private static long aLargo(Object valor) {
        if (valor == null) return 0;
        if (valor instanceof Number) return ((Number) valor).longValue();
        try {
            return Long.parseLong(valor.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
