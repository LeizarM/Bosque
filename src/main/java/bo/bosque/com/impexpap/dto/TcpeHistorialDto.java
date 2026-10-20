package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Fila de {@code p_list_tcpe_Resultado 'H'}: historial de exámenes de una persona por CI.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeHistorialDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codExamen;

    private Long codRolExamen;

    private String tituloLote;

    private String nombreClasificacion;

    private Date fechaInicio;

    private Date fechaFin;

    private String estado;

    private Double resultado;

    private String login;

    private String nombreCompleto;

    private String ci;

    private BigDecimal notaAprobacion;

    /** {@code resultado >= notaAprobacion} del lote. */
    private Boolean aprobado;
}
