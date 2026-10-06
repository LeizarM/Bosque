package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Fila de tac_planillaIncapacidad: una baja médica con su descuento por incapacidad.
 * Los importes los calcula p_abm_planillaIncapacidad (ACCION A); la app solo marca fueRevisado.
 */
@Data
@NoArgsConstructor
public class PlanillaIncapacidad {
    private Long idPIT;
    private Long codPermiso;
    private String numSeguro;
    private String motivo;
    private String datoEmpleado;
    private Double salarioMensual;
    private Double salarioDiario;
    private Double porcentajeAl75;
    private Long nroBaja;
    private Date desde;
    private Date hasta;
    private Integer diasBaja;
    private Integer diasAsumidosCordes;
    private Double totalDescuento;
    private Integer fueRevisado;
    private Date fechaRevisado;
    private Long audUsuario;
    private Date audFecha;

    // Campos calculados por el SP (JOIN trh_afiliacion / trh_seguro, ACCION A)
    private Long codSeguro;
    private String seguro;
}
