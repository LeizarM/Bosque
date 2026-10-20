package bo.bosque.com.impexpap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Respuesta del login del postulante. {@code expiracion} es el vencimiento del token.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TcpeSesionPostulanteDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    private Date expiracion;

    private Long codUsuario;

    private String login;

    /** false = debe completar su registro antes de rendir. */
    private Boolean datosCompletos;

    private String nombres;
}
