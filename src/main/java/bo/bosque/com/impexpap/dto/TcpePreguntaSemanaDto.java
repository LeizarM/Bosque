package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_PreguntaSemana 'L'} para la gestión (contrato 15.3): la tabla, el estado de hoy
 * ({@code INACTIVA}, {@code PROGRAMADA}, {@code TERMINADA} o {@code VIGENTE}) y cuántos respondieron.
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

    private Long audUsuario;

    private Date audFecha;

    private String nombreUsuario;

    /** {@code titulo} o, si no tiene, el título de la configuración. */
    private String tituloMostrado;

    private String estado;

    private Integer respuestas;
}
