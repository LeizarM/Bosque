package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila de {@code p_list_tcpe_PreguntaSemana 'V'} (inicio) más sus opciones de {@code 'O'}. {@code esCorrecta},
 * {@code respuestaCorrecta} y {@code mensaje} los decide el SP: llegan solo después de responder.
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

    private Boolean mostrarRespuesta;

    private Boolean respondida;

    private Long codRsptaOpElegida;

    private Boolean esCorrecta;

    private String respuestaCorrecta;

    private String mensaje;

    /** Cuántas personas respondieron en total. */
    private Integer respuestas;

    private List<TcpePreguntaSemanaOpcionDto> opciones = new ArrayList<>();
}
