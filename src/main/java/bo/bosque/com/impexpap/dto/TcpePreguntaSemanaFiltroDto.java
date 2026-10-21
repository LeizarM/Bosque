package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Cuerpo de {@code /examenes/pregunta-semana/*} salvo {@code guardar}: cada endpoint usa una parte. Solo tipos: las
 * reglas (vigencia, opción de la pregunta, una respuesta por persona) las decide el SP.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpePreguntaSemanaFiltroDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPreguntaSemana;

    /** {@code listar}: filtro; {@code activar}: el valor nuevo. */
    private Boolean activo;

    /** {@code responder}: la opción elegida. */
    private Long codRsptaOp;

    /** {@code banco}: categoría. */
    private Long codClasificacion;

    /** {@code banco}: busca dentro del texto de la pregunta. */
    @Size(max = 200, message = "Máximo 200 caracteres.")
    private String texto;
}
