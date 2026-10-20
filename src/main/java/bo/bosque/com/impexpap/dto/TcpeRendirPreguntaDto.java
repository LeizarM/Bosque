package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Rendir 'P'} (solo con el examen en curso).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRendirPreguntaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPreguntaH;

    private Integer orden;

    private String pregunta;

    private Boolean esCerrada;

    private Long codRespuestaH;

    private String respuestaU;

    private Boolean respondida;
}
