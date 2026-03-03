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
    private float montoFacturaUsd;
    private float montoAmortizadoUsd;
    private float montoAPagarUsd;
    private Date fechaFactura;
    private Date fechaVencimiento;
    private String concepto;
    private String obs;
    private int esAprobado;
    private int audUsuario;

    private int codEmpresa;


}