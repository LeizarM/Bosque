package bo.bosque.com.impexpap.security.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * El usuario del token tal como lo carga {@link JwtTokenFilter} en cada pedido, con el estado de la sesión del token.
 *
 * <p>Sale de una sola consulta ({@code p_list_Usuario 'B'} con {@code @idSesion}): antes eran dos, la del usuario y
 * otra a {@code trec_sesion}. Es un {@link User}, así que quien lea el principal como tal no cambia.
 */
public class UsuarioToken extends User {

    private final boolean sesionCerrada;
    private final String motivoCierreSesion;

    public UsuarioToken(String login, String password, boolean relacionActiva, boolean noBloqueadoPorAdministrador,
                        Collection<? extends GrantedAuthority> authorities, boolean sesionCerrada, String motivoCierreSesion) {
        super(login, password, relacionActiva, true, true, noBloqueadoPorAdministrador, authorities);
        this.sesionCerrada = sesionCerrada;
        this.motivoCierreSesion = motivoCierreSesion;
    }

    /** La sesión del token ya no vale: cerrada, inexistente o de otro usuario. Sin sid en el token, false. */
    public boolean isSesionCerrada() { return sesionCerrada; }

    /** S, A, B, I o R; null si la sesión no existe o es de otro usuario. */
    public String getMotivoCierreSesion() { return motivoCierreSesion; }
}
