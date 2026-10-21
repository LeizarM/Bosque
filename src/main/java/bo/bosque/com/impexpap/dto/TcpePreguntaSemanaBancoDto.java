package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_PreguntaSemana 'B'}: una pregunta del banco para elegir. {@code puedeElegirse} y
 * {@code motivo} salen de {@code f_tcpe_motivoSemana}; {@code vecesUsada} = preguntas de la semana que ya la usaron.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpePreguntaSemanaBancoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPregunta;

    private Long codClasificacion;

    private String nombreClasificacion;

    private String nombreArea;

    private String pregunta;

    private Integer cantOpciones;

    private Integer cantValidas;

    private Boolean puedeElegirse;

    /** Por qué no se puede elegir; nulo si se puede. */
    private String motivo;

    private Integer vecesUsada;
}
