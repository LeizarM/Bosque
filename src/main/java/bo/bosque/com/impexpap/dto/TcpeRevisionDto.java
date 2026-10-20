package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Resultado 'R'}: revisión pregunta por pregunta de un examen.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRevisionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPreguntaH;

    private Long codRespuestaH;

    private Integer orden;

    private String pregunta;

    private Boolean esCerrada;

    private String respuestaModelo;

    private String respuestaU;

    private Boolean esCorrecta;

    /** Textos de las opciones marcadas, unidos con ' | '. */
    private String marcadas;

    /** Textos de las opciones válidas, unidos con ' | '. */
    private String correctas;
}
