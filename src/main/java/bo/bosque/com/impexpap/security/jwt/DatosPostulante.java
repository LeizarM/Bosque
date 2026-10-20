package bo.bosque.com.impexpap.security.jwt;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/**
 * La identidad de un <b>postulante externo</b> del módulo de exámenes, colgada del {@code Authentication}.
 *
 * <h3>Por qué no es un {@link DatosToken}</h3>
 * El {@code jti} del token del postulante es el {@code codUsuario} de {@code tcpe_usuario}, no el de
 * {@code tb_usuario}. Si viajara como {@code DatosToken}, cualquier código que hace
 * {@code DatosToken.codUsuarioDe(auth)} (por ejemplo {@code /auth/sesion} o {@code /auth/salir}, que son
 * {@code permitAll}) lo tomaría por el empleado que tenga ese mismo número. Con un tipo aparte,
 * {@code DatosToken.deONull} devuelve null para un postulante y {@code DatosToken.de} corta con 403.
 *
 * <p>Falla cerrado igual que {@code DatosToken}: sin token de postulante, {@link #de} tira 403.
 */
public final class DatosPostulante {

    /** La authority del token del postulante; {@code hasRole('POSTULANTE')} la reconoce. */
    public static final String ROL = "ROLE_POSTULANTE";

    private final long codUsuario;
    private final String login;

    public DatosPostulante(long codUsuario, String login) {
        this.codUsuario = codUsuario;
        this.login = login;
    }

    /** {@code tcpe_usuario.codUsuario} del postulante. */
    public long getCodUsuario() { return codUsuario; }

    public String getLogin() { return login; }

    /** Los datos del postulante que llama, o 403. */
    public static DatosPostulante de(Authentication auth) {
        Object detalles = auth != null ? auth.getDetails() : null;
        if (!(detalles instanceof DatosPostulante) || ((DatosPostulante) detalles).codUsuario <= 0) {
            throw new AccessDeniedException("Su sesión de examen no es válida. Vuelva a ingresar.");
        }
        return (DatosPostulante) detalles;
    }

    /** El {@code codUsuario} de {@code tcpe_usuario} del postulante que llama. */
    public static long codUsuarioDe(Authentication auth) {
        return de(auth).getCodUsuario();
    }

    @Override
    public String toString() {
        return "DatosPostulante{codUsuario=" + codUsuario + ", login='" + login + "'}";
    }
}
