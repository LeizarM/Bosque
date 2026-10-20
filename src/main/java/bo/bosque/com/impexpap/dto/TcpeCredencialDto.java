package bo.bosque.com.impexpap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Credencial de un examen recién generado. La clave se muestra una sola vez: en la base solo queda su hash. Para un empleado {@code login} y {@code password} van nulos (rinde con su sesión).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TcpeCredencialDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codExamen;

    private String login;

    private String password;

    private Long codEmpleado;

    private String nombreCompleto;

    private String ci;
}
