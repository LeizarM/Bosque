package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_PreguntaSemana 'O'}: una opción de una pregunta vigente para quien mira, con la que
 * eligió. {@code esValido} es nulo hasta que responde, y siempre si la pregunta no muestra la respuesta.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpePreguntaSemanaOpcionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPreguntaSemana;

    private Long codRsptaOp;

    private String opcionRspta;

    private Boolean elegida;

    private Boolean esValido;
}
