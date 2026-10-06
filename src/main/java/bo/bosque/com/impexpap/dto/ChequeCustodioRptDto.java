package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una fila de {@code RptChequeCajaSalida} ("Reporte Custodio"), de {@code p_list_Cheque} 'S'. {@code facturas}
 * son las notas de remision del cheque ({@code p_list_NotaRemision} 'A'): en el legacy las traia el subreporte
 * con su propia consulta; aqui van dentro de la fila y el subreporte las recibe como coleccion.
 * Ojo: {@code fechaCobrar} es TEXTO en este reporte (la fecha de la transaccion bancaria, dd/MM/yyyy).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChequeCustodioRptDto implements Serializable {

    private Long fila;
    private Timestamp fecha;
    private String cliente;
    private String banco;
    private String nrocheque;
    private String fechaCobrar;
    private Double monto;
    private String moneda;
    private Integer codCheque;
    private String nroTransaccion;
    private String datoBanco;
    private List<NotaRemisionRptDto> facturas;
}
