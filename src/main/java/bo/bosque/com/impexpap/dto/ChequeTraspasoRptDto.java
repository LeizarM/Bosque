package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una fila de la nomina de un traspaso: {@code RptChequeEntregaCaja} (el ultimo traspaso, {@code p_list_Cheque}
 * 'T') y {@code RptChkTraspasoAdm} (la reimpresion de uno elegido, 'H'). Mismas columnas en las dos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChequeTraspasoRptDto implements Serializable {

    private Long fila;
    private Timestamp fecha;
    private String cliente;
    private String reciboManual;
    private String responsableRecojo;
    private String banco;
    private String nrocheque;
    private Date fechaCobrar;
    private Double monto;
    private String moneda;
    private String observacion;
}
