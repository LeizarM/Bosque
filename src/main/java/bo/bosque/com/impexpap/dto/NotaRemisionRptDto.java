package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una nota de remision de un cheque ({@code p_list_NotaRemision} 'A'), para el subreporte
 * {@code subRptChequeCajaSalidaFact} del reporte de custodio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotaRemisionRptDto implements Serializable {

    private Integer codCheque;
    private String notaRemision;
    private Integer nroFactura;
    private Date fechaFactura;
    private String fechaString;
}
