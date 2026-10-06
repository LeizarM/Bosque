package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.*;

/**
 * Fila del reporte <b>RptCobrRec</b> (recibo de una garantia). Sale de
 * {@code p_list_GarantiaCbr} ACCION 'D' con {@code @codGarantia}.
 *
 * <p><b>Los tipos NO son libres:</b> tienen que ser exactamente las clases de los
 * {@code <field>} del .jrxml ({@code java.sql.Date}, no {@code java.util.Date}), porque
 * {@code JRBeanCollectionDataSource} entrega el valor tal cual y el reporte compilado lo
 * castea a la clase declarada.
 *
 * <p>La rama 'D' hace JOIN directo con {@code v_clientesSAP}, que trae una fila por empresa
 * SAP: el DAO se queda con una sola por {@code codGarantia}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ReciboGarantiaRptDto implements Serializable {

    private String nroRecibo;
    private String cliente;
    private BigDecimal monto;
    private BigDecimal lineaCredito;
    private String tiempoDePago;
    private String tipoDeGarantia;
    private java.sql.Date fechaInicio;
    private java.sql.Date fechaExpiracion;
    private String datoCliente;
    private String datoEstado;
    private String lineaCreditoSAP;
    private String realizoEmp;
    private java.sql.Date fechaRecepcion;
    private String descripcion;
    private BigDecimal tipoCambioBs;
    private Long codGarantia;

    /** Datos del subreporte subRptCbrzDetalle (antes lo llenaba su propia consulta). */
    private List<DetalleReciboRptDto> detalles = new ArrayList<>();
}
