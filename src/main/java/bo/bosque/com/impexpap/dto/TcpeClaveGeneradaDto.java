package bo.bosque.com.impexpap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Respuesta de {@code /examenes/usuarios/regenerar-clave}. La clave se muestra una sola vez.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TcpeClaveGeneradaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codUsuario;

    private String login;

    private String password;

    private String ci;

    private String nombreCompleto;
}
