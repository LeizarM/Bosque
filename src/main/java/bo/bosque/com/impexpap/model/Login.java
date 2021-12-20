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



    private Empleado emp = new Empleado();

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

    public Login(int codUsuario, int codEmpleado, String login, String password, String tipoUsuario, String esAutorizador, String estado, int audUsuarioI, Empleado emp) {
        this.codUsuario = codUsuario;
        this.codEmpleado = codEmpleado;
        this.login = login;
        this.password = password;
        this.tipoUsuario = tipoUsuario;
        this.esAutorizador = esAutorizador;
        this.estado = estado;
        this.audUsuarioI = audUsuarioI;
        this.emp = emp;
    }
}