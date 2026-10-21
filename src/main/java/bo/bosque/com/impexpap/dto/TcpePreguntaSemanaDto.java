package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_PreguntaSemana 'L'} para la gestión (contrato 15.3): la tabla, la pregunta del banco,
 * el estado de hoy ({@code INACTIVA}, {@code PROGRAMADA}, {@code TERMINADA} o {@code VIGENTE}) y los aciertos.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpePreguntaSemanaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPreguntaSemana;

    private Long codPregunta;

    private String titulo;

    @FechaDiaJson
    private LocalDate fechaInicio;

    @FechaDiaJson
    private LocalDate fechaFin;

    private Boolean mostrarRespuesta;

    private Boolean activo;

    private Long audUsuario;

    private Date audFecha;

    private String nombreUsuario;

    /** {@code titulo} o, si no tiene, el título de la configuración. */
    private String tituloMostrado;

    private String pregunta;

    private Long codClasificacion;

    private String nombreClasificacion;

    private String estado;

    private Integer respuestas;

    private Integer correctas;

    /** Nulo mientras nadie respondió. */
    private BigDecimal porcentajeAcierto;
}
