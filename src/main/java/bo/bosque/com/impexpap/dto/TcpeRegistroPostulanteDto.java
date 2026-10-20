package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Cuerpo de {@code /examen-publico/registro}: el postulante completa sus datos.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRegistroPostulanteDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Es obligatorio.")
    @Size(max = 20, message = "Máximo 20 caracteres.")
    private String ci;

    @NotBlank(message = "Es obligatorio.")
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String nombres;

    // Basta un apellido, paterno o materno: lo exige p_abm_tcpe_Acceso 'R'.
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String apellidoPaterno;

    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String apellidoMaterno;
}
