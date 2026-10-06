package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una fila de {@code RptChequeCobranzas} ("Reporte Cheques"), tal como la entrega {@code p_list_Cheque} 'P'.
 * La columna 13 del SP ({@code descripcion}, el estado) no la usa la plantilla y no se lee.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChequeCobranzaRptDto implements Serializable {

    private Long fila;
    private Timestamp fecha;
    private String facturaDeReserva;
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
