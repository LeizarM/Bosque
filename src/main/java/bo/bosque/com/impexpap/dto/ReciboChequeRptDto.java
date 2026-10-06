package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * El recibo de caja de un cheque: {@code RptCheqCajRec}, de {@code p_list_Cheque} 'D' (una sola fila).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReciboChequeRptDto implements Serializable {

    private Integer codCheque;
    private String nroRecibo;
    private Timestamp fecha;
    private String cliente;
    private String banco;
    private String nroCheque;
    private Double monto;
    private String montoLiteral;
    private String moned;
    private String datosCajero;
    private String datosEmp;
}
