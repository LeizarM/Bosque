package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_PreguntaSemana 'L'} para la gestión (contratos 16 y 17): la tabla, el estado de hoy
 * ({@code INACTIVA}, {@code PROGRAMADA}, {@code TERMINADA} o {@code VIGENTE}), cuántos respondieron y, si es de
 * escala, el promedio.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpePreguntaSemanaDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codPreguntaSemana;

    private String titulo;

    private String pregunta;

    @FechaDiaJson
    private LocalDate fechaInicio;

    @FechaDiaJson
    private LocalDate fechaFin;

    private Boolean activo;

    /** A abierta o E escala. */
    private String tipo;

    private Integer escalaMin;

    private Integer escalaMax;

    private String etiquetaMin;

    private String etiquetaMax;

    private Long audUsuario;

    private Date audFecha;

    private String nombreUsuario;

    /** {@code titulo} o, si no tiene, el título de la configuración. */
    private String tituloMostrado;

    /** "Abierta" o "Escala del 1 al 5", armado por el SP. */
    private String tipoMostrado;

    private String estado;

    private Integer respuestas;

    /** Solo escala: el promedio con un decimal; nulo si nadie respondió. */
    private Double promedio;
}
