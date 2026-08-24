package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

/**
 * Tipo de cambio sugerido para el preliminar.
 * <p>
 * origen dice de donde salio: SAP, HISTORICO o POR DEFECTO. diasDeAntiguedad,
 * cuantos dias tiene la cotizacion respecto de la fecha pedida. Los dos evitan
 * que la pantalla muestre como dato del dia algo que puede tener anios.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TipoCambioComision implements Serializable {

    private Date       fecha;
    private BigDecimal tipoCambio;
    private String     origen;
    private Integer    diasDeAntiguedad;
}
