package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una fila de {@code RptChequeCajaRecepcion} ("Reporte": cheques recibidos en caja), tal como la entrega
 * {@code p_list_Cheque} 'R'. Los nombres y TIPOS son los {@code <field>} de la plantilla: Jasper los lee por
 * reflexion de los getters y un tipo distinto revienta al llenar ({@code ClassCastException}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChequeRecibidoRptDto implements Serializable {

    private Long fila;
    private Timestamp fecha;
    private Timestamp fechaTraspaso;
    private String nroRecibo;
    private String cliente;
    private String reciboManual;
    private String responsableRecojo;
    private String banco;
    private String nrocheque;
    private Date fechaCheque;
    private Double monto;
    private String moneda;
    private String observacion;
}
