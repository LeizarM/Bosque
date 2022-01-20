package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Login implements Serializable, UserDetails {

    private int codUsuario;
    private int codEmpleado;
    private String login;
    private String password;
    private String tipoUsuario;
    private String esAutorizador;
    private String estado;
    private int audUsuarioI;
    private String elTemaSelecionado = "";

    /**
     * Variables Auxiliares
     */
    private Empleado empleado = new Empleado();
    private Sucursal sucursal = new Sucursal();
    private Collection<? extends GrantedAuthority> authorities;
    /**
     * Constructores
     */
    public Login(int codUsuario, int codEmpleado, String login, String password, String tipoUsuario,String esAutorizador, String estado, int audUsuarioI) {

        this.codUsuario = codUsuario;
        this.codEmpleado = codEmpleado;
        this.login = login;
        this.password = password;
        this.tipoUsuario = tipoUsuario;
        this.esAutorizador = esAutorizador;
        this.estado = estado;
        this.audUsuarioI = audUsuarioI;
    }

    public Login( int codUsuario,  String datoPersona , int codSucursal, String nombre, int codCiudad, String datoCiudad, String descripcionCargo, String tipoUsuario, int codEmpresa, String nombreEmpresa, String elTemaSelecionado, Collection<? extends GrantedAuthority> authorities ) {
        this.codUsuario = codUsuario;
        this.empleado.getPersona().setDatoPersona( datoPersona );

        this.sucursal.setCodSucursal( codSucursal );
        this.sucursal.setNombre( nombre );

        this.sucursal.setCodCiudad( codCiudad );
        this.sucursal.setNombreCiudad( datoCiudad );

        this.empleado.getCargo().setDescripcion( descripcionCargo );
        this.tipoUsuario = tipoUsuario;

        this.sucursal.setCodEmpresa( codEmpresa );
        this.sucursal.setNombreEmpresa( nombreEmpresa );

        this.elTemaSelecionado = elTemaSelecionado;

        this.authorities = authorities;
    }


    /**
     *   ========================  IMPLEMENTANDO METODOS DE LA INTERFAZ =====================
     *   ====================================================================================
     */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getUsername() {
        return this.login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}