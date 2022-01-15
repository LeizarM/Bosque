package bo.bosque.com.impexpap.security.model;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;


@Getter
@Setter
@NoArgsConstructor
@ToString
public class Jwt {

    private String token;
    private String bearer = "Bearer";
    private String nombreCompleto;
    private String cargo;
    private String tipoUsuario;
    private int codUsuario;
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Constructor
     */
    public Jwt(String token, String nombreCompleto, String cargo, String tipoUsuario, int codUsuario, Collection<? extends GrantedAuthority> authorities) {
        this.token = token;
        this.nombreCompleto = nombreCompleto;
        this.cargo = cargo;
        this.tipoUsuario = tipoUsuario;
        this.codUsuario = codUsuario;
        this.authorities = authorities;
    }
}
