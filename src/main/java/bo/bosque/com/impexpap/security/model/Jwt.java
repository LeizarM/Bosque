package bo.bosque.com.impexpap.security.model;

import java.io.Serializable;
import java.util.Collection;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;


@Getter
@Setter
@NoArgsConstructor
@ToString
public class Jwt implements Serializable {

    private String token;
    private String bearer = "Bearer";
    private String nombreCompleto;
    private String cargo;
    private String tipoUsuario;
    private int codUsuario;
    private int codEmpleado;
    private int codEmpresa;
    private String login;
    private Collection<? extends GrantedAuthority> authorities;
    
    /**
     * Constructor
     */
    public Jwt(String token, String nombreCompleto, String cargo, String tipoUsuario, int codUsuario, int codEmpleado,int codEmpresa , String login, Collection<? extends GrantedAuthority> authorities) {
        this.token = token;
        this.nombreCompleto = nombreCompleto;
        this.cargo = cargo;
        this.tipoUsuario = tipoUsuario;
        this.codUsuario = codUsuario;
        this.codEmpleado = codEmpleado;
        this.codEmpresa = codEmpresa;
        this.login = login;
        this.authorities = authorities;
    }
}
