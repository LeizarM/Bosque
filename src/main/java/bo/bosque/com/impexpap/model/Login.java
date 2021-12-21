package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Login implements Serializable {

    private int codUsuario;
    private int codEmpleado;
    private String login;
    private String password;
    private String tipoUsuario;
    private String esAutorizador;
    private String estado;
    private int audUsuarioI;


    private String elTemaSelecionado = "";
    private Empleado empleado = new Empleado();
    private Sucursal sucursal = new Sucursal();

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

    public Login( int codUsuario,  String datoPersona , int codSucursal, String nombre, int codCiudad, String datoCiudad, String descripcionCargo, String tipoUsuario, int codEmpresa, String nombreEmpresa, String elTemaSelecionado ) {
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
    }



}