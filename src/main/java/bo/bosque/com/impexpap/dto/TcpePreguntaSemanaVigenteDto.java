package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_PreguntaSemana 'V'} (inicio): la pregunta vigente, lo que respondió quien mira y, para
 * el campo de texto, la ayuda y el largo máximo. {@code mensaje} (el agradecimiento) llega solo ya respondida.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpePreguntaSemanaVigenteDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPreguntaSemana;

    /** Ya trae el título de la configuración si la pregunta no tiene uno. */
    private String titulo;

    private String pregunta;

    @FechaDiaJson
    private LocalDate fechaInicio;

    @FechaDiaJson
    private LocalDate fechaFin;

    private Integer diasRestantes;

    private Boolean respondida;

    /** Lo que escribió quien mira; nulo si todavía no respondió. */
    private String respuesta;

    private Date fechaRespuesta;

    private String mensaje;

    private String ayuda;

    private Integer maxRespuesta;
}
