package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Monitor 'E'} (contrato 8.4): espejo de la pregunta que el evaluado tiene delante, una
 * fila por opción, con lo que marcó. El evaluador sí ve {@code esValido}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeMonitorEspejoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codExamen;

    private String nombreCompleto;

    private String estado;

    private Long segundosRestantes;

    private String conexion;

    private Integer totalPreguntas;

    private Integer respondidas;

    private Integer ordenActual;

    private Long codPreguntaH;

    private String pregunta;

    private Boolean esCerrada;

    private String respuestaU;

    private Long codRsptaOpH;

    private String opcionRspta;

    private Boolean marcada;

    private Boolean esValido;
}
