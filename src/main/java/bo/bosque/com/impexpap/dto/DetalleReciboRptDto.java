package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.*;

/**
 * Fila del subreporte <b>subRptCbrzDetalle</b> (detalles de la garantia en el recibo).
 * Sale de {@code p_list_CbrDetalle} ACCION 'A'. Tipos iguales a los {@code <field>} del
 * .jrxml, por la misma razon que en {@link ReciboGarantiaRptDto}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class DetalleReciboRptDto implements Serializable {

    private Long codDetalle;
    private Long codGarantia;
    private java.sql.Timestamp fecha;
    private String tipoGarantia;
    private String detalle;
    private BigDecimal montoGarantiaParc;
    private String datoGarantia;
    private String fechaString;
}
