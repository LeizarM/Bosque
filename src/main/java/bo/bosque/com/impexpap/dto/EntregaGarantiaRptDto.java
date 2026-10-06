package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.*;

/**
 * Fila del reporte <b>RptCbrEntregaGarantias</b> (nomina del ultimo traspaso a custodia).
 * Sale de {@code p_list_GarantiaCbr} ACCION 'T'. Tipos iguales a los {@code <field>} del
 * .jrxml.
 *
 * <p>La rama 'T' repite cada garantia una vez por empresa SAP del cliente: el DAO se queda
 * con una por {@code nroRecibo} y vuelve a numerar {@code fila}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class EntregaGarantiaRptDto implements Serializable {

    private Long fila;
    private String nroRecibo;
    private String cliente;
    private BigDecimal monto;
    private BigDecimal lineaCredito;
    private String tiempoDePago;
    private String tipoDeGarantia;
    private java.sql.Date fechaInicio;
    private java.sql.Date fechaExpiracion;
    private String datoGarantia;
    private String datoCliente;
    private String datoEstado;
    private String lineaCreditoSAP;
    private String realizoEmp;
    private java.sql.Date fechaRecepcion;
    private String descripcion;
    private Integer tipoCamb;
}
