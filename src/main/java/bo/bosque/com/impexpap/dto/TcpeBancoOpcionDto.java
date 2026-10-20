package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Opción de una pregunta cerrada: fila de {@code p_list_tcpe_Banco 'O'} y, al guardar, cada elemento de {@code opciones}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoOpcionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 0 o nulo = opción nueva. */
    private Long codRsptaOp;

    private Long codRespuesta;

    private Long codPregunta;

    @NotBlank(message = "Es obligatorio.")
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String opcionRspta;

    /** Si es una respuesta correcta. */
    private Boolean esValido;

    /** Solo lectura: si algún examen la registró. */
    private Boolean usadaEnExamenes;
}
