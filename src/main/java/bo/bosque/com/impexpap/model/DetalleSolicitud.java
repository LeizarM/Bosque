package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DetalleSolicitud implements Serializable {

    private long idDetalle;
    private long idSolicitudProveedor;
    private String tipoDocumento;  // Proforma, Factura, etc.
    private String numeroDocumento; // el numero de factura o proforma ( si es que tiene )
    private int facturaProvSap; // docNum SAP
    private String codigoImportacion;
    private double montoFacturaUsd;      // decimal(18,2) en BD
    private double montoAmortizadoUsd;   // decimal(18,2) en BD
    private double montoAPagarUsd;       // decimal(18,2) en BD — esta cuota
    private double montoTotalDocumento;  // decimal(18,2) — total del doc SAP (DocTotal de OPCH/OPOR)
    private int numeroCuota;             // 1, 2, 3... permite varias cuotas por mismo facturaProvSap
    private Date fechaFactura;
    private Date fechaVencimiento;
    private String concepto;
    private String obs;
    private int esAprobado;
    private int audUsuario;

    private int codEmpresa;


}