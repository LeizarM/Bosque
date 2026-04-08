package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
public class DetalleSolicitudDto {
    private long idDetalle;
    private long idSolicitudProveedor;
    private String tipoDocumento;
    private String numeroDocumento;
    private int facturaProvSap;
    private String codigoImportacion;
    private double montoFacturaUsd;      // decimal(18,2) en BD
    private double montoAmortizadoUsd;   // decimal(18,2) en BD
    private double montoAPagarUsd;       // decimal(18,2) en BD
    private Date fechaFactura;
    private Date fechaVencimiento;
    private String concepto;
    private String obs;
    private int esAprobado;
    private int audUsuario;
    private int codEmpresa;
}