package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Rendir 'F'}: devolución de un examen ya entregado a quien lo rindió. Una fila por
 * opción; las abiertas, una sola con la opción en NULL. {@code esValido} es NULL si el lote no muestra respuestas.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRevisionItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPreguntaH;

    private Integer orden;

    private String pregunta;

    private Boolean esCerrada;

    /** De la pregunta, no de la opción. */
    private Boolean esCorrecta;

    private String respuestaU;

    private Long codRsptaOpH;

    private String opcionRspta;

    private Boolean marcada;

    private Boolean esValido;
}
