package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
public class ReportePlanoDto {
    // --- Cabecera ---
    private long idSolicitud;
    private int codEmpresa;
    private String nombre;
    private Date fechaSolicitud;
    private double montoTotalSolicitud;  // decimal(18,2) en BD
    private String estado;

    // --- Proveedor ---
    private long idSolicitudProveedor;
    private String cardCode;
    private String cardName;
    private double totalFacturasUsd;     // decimal(18,2) en BD
    private double totalAmortizadoUsd;   // decimal(18,2) en BD
    private double totalAPagarUsd;       // decimal(18,2) en BD

    // --- Detalle ---
    private long idDetalle;
    private String tipoDocumento;
    private String numeroDocumento;
    private String facturaProvSap;
    private String codigoImportacion;
    private double montoFacturaUsd;      // decimal(18,2) en BD
    private double montoAmortizadoUsd;   // decimal(18,2) en BD — ¡CUIDADO! nombre igual que proveedor, OK porque BeanUtils copia al DTO correcto
    private double montoAPagarUsd;       // decimal(18,2) en BD
    private Date fechaFactura;
    private Date fechaVencimiento;
    private String concepto;
    private String obs;
    private int esAprobado;
}