package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_Resultado 'L'}: un examen del lote con su estado y su nota.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeResultadoExamenDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codExamen;

    private Long codRolExamen;

    private Long codUsuario;

    private String login;

    private Long codEmpleado;

    private String nombreCompleto;

    private String ci;

    /** SIN_INICIAR, EN_CURSO, VENCIDO, POR_REVISAR, CALIFICADO o AUSENTE (no inició dentro de la ventana). */
    private String estado;

    private Date fechaInicio;

    private Date fechaFin;

    private Date fechaLimite;

    private Double resultado;

    /** {@code resultado >= notaAprobacion} del lote. */
    private Boolean aprobado;

    private BigDecimal notaAprobacion;

    private Integer totalPreguntas;

    private Integer respondidas;

    private Integer abiertasPendientes;
}
