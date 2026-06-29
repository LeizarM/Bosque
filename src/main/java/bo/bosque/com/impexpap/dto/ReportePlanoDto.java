package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
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
    private String project;              // Código del proyecto SAP

    // --- Proveedor ---
    private long idSolicitudProveedor;
    private String cardCode;
    private String cardName;
    // BigDecimal para que coincida con SolicitudProveedorDto y BeanUtils.copyProperties
    // los copie (double -> BigDecimal NO es asignable y se omitía, dejando los totales en 0).
    private BigDecimal totalFacturasUsd;     // decimal(18,2) en BD
    private BigDecimal totalAmortizadoUsd;   // decimal(18,2) en BD
    private BigDecimal totalAPagarUsd;       // decimal(18,2) en BD
    private String estadoProveedor;      // estado del proveedor (PENDIENTE/APROBADO/RECHAZADO)

    // --- Detalle ---
    private long idDetalle;
    private String tipoDocumento;
    private String numeroDocumento;
    private int facturaProvSap;          // docNum SAP (int) — debe coincidir con DetalleSolicitudDto para que BeanUtils.copyProperties lo copie
    private String codigoImportacion;
    private int numeroCuota;             // 1, 2, 3...
    private double montoFacturaUsd;      // decimal(18,2) en BD
    private double montoAmortizadoUsd;   // decimal(18,2) en BD — ¡CUIDADO! nombre igual que proveedor, OK porque BeanUtils copia al DTO correcto
    private double montoAPagarUsd;       // decimal(18,2) en BD
    private double montoTotalDocumento;  // decimal(18,2) — total del doc SAP
    private Date fechaFactura;
    private Date fechaVencimiento;
    private String concepto;
    private String obs;
    private int esAprobado;
}