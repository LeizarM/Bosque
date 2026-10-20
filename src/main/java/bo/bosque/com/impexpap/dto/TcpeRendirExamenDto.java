package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_Rendir} {@code M} (mis exámenes) y {@code E} (uno). {@code segundosRestantes} lo calcula el servidor; {@code resultado} solo viene si el estado es {@code CALIFICADO}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRendirExamenDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codExamen;

    private Long codRolExamen;

    private String titulo;

    private String nombreClasificacion;

    private String modalidad;

    private Integer duracionMinutos;

    private Integer totalPreguntas;

    private Integer respondidas;

    /** SIN_INICIAR, EN_CURSO, VENCIDO, POR_REVISAR, CALIFICADO o AUSENTE. */
    private String estado;

    /** Texto para el evaluado en ese estado; lo redacta SQL. */
    private String mensajeEstado;

    private Date fechaInicio;

    private Date fechaFin;

    private Date fechaLimite;

    /** Ventana del lote: antes de {@code inicioVentana} no se puede iniciar. Nulas = sin límite. */
    private Date inicioVentana;

    private Date finVentana;

    private Long segundosRestantes;

    private Double resultado;

    private BigDecimal notaAprobacion;

    /** {@code resultado >= notaAprobacion}; solo viene con el examen calificado. */
    private Boolean aprobado;

    /** El lote deja ver las respuestas correctas en la revisión al terminar. */
    private Boolean mostrarRespuestas;
}
