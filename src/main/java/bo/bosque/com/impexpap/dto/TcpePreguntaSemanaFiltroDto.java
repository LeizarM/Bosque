package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Cuerpo de {@code /examenes/pregunta-semana/*} salvo {@code guardar}: cada endpoint usa una parte. Solo tipos: las
 * reglas (vigencia, largo de la respuesta, una respuesta por persona) las decide el SP.
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

    /** {@code responder} una abierta: lo que escribió quien responde. */
    @Size(max = 1000, message = "Máximo 1000 caracteres.")
    private String respuesta;

    /** {@code responder} una de escala: el número elegido. */
    private Integer valor;

    /** {@code quitar-respuesta}: la respuesta que se quita. */
    private Long codRsptaSemana;
}
