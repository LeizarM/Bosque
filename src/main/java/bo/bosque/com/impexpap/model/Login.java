package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Collection;

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
    private String password2;
    private String tipoUsuario;
    private String esAutorizador;
    private String estado;
    private int audUsuarioI;
    private String elTemaSelecionado = "";
    private String npassword;
    private String versionApp;

    /*
    **** Variables de apoyo
     */
   private int codSucursal;
   private String nombreSucursal;
   private int codCiudad;
   private String nombreCiudad;
   private int codEmpresa;
   private String nombreEmpresa;



    /**
     * Variables Auxiliares
     */
    private Empleado empleado = new Empleado();

    private Collection<? extends GrantedAuthority> authorities;
    /**
     * Constructores
     */
    public Login(int codUsuario, int codEmpleado, String login, String password, String password2, String npassword ,String tipoUsuario,String esAutorizador, String estado, int audUsuarioI) {

        this.codUsuario = codUsuario;
        this.codEmpleado = codEmpleado;
        this.login = login;
        this.password = password;
        this.password2 = password2;
        this.npassword = npassword;
        this.tipoUsuario = tipoUsuario;
        this.esAutorizador = esAutorizador;
        this.estado = estado;
        this.audUsuarioI = audUsuarioI;
    }

    public Login( int codUsuario,  String datoPersona , int codSucursal, String nombreSucursal, int codCiudad, String datoCiudad, String descripcionCargo, String tipoUsuario, int codEmpresa, String nombreEmpresa, String elTemaSelecionado, String versionApp ,Collection<? extends GrantedAuthority> authorities ) {
        this.codUsuario = codUsuario;
        this.empleado.getPersona().setDatoPersona( datoPersona );

        this.codSucursal = codSucursal;
        this.nombreSucursal =  nombreSucursal;

        this.codCiudad = codCiudad;
        this.nombreCiudad = datoCiudad;

        this.empleado.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion( descripcionCargo );
        this.tipoUsuario = tipoUsuario;

        this.codEmpresa = codEmpresa;
        this.nombreEmpresa =  nombreEmpresa;

        this.elTemaSelecionado = elTemaSelecionado;

        this.versionApp = versionApp;

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