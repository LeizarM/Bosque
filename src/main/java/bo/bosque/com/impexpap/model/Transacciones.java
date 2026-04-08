package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Transacciones implements Serializable {

    // ── campos de la tabla tpex_Transacciones ──────────────────────────────
    private long idTransaccion;
    private String numeroTransaccion;
    private long idSolicitud;
    private long idCotizacion;
    private long idTipoTransaccion;
    private int codBanco;
    private long idCanal;                    // bigint en BD (era int)
    private long codEmpresa;                 // bigint en BD (era int)
    private String cardCode;
    private Date fechaTransaccion;
    private Date fechaValor;
    private double montoOrigen;              // decimal(18,2) en BD
    private long idMonedaOrigen;             // bigint en BD (era int)
    private double tipoCambioAplicado;       // decimal(10,6) en BD
    private double montoConvertido;          // decimal(18,2) en BD
    private long idMonedaDestino;            // bigint en BD (era int)
    private double totalCargos;              // decimal(18,2) en BD
    private double totalFinal;               // decimal(18,2) en BD
    private String numeroContrato;
    private Date fechaPactado;
    private Date fechaVencimiento;
    private double tipoCambioForward;        // decimal(10,6) en BD
    private double tipoCambioReferencia;     // decimal(10,6) en BD
    private double equivalenteUsdRef;        // decimal(18,2) en BD
    private double diferenciaDeMas;          // decimal(18,2) en BD
    private double porcentajeDiferencia;     // decimal(10,4) en BD
    private String nombreExportadora;
    private double tcNegociadoExportadora;   // decimal(10,6) en BD
    private double comisionExportadora;      // decimal(18,2) en BD
    private String metodoExportadora;
    private String estado;
    private String observaciones;
    private long audUsuario;                 // bigint en BD (era int)
    private String  rutaVoucher;             // ruta relativa del archivo voucher
    private Boolean tieneVoucher;            // columna calculada desde BD (solo lectura)

    // ── campos derivados de JOINs (devueltos por el SP) ───────────────────
    private String tipoTransaccion;
    private Boolean requiereForward;
    private Boolean requiereBanco;
    private String banco;
    private String proveedor;
    private String monedaOrigen;
    private String monedaDestino;
    private String empresa;

    // ── filtros de rango de fecha (acción B) ──────────────────────────────
    private Date fechaInicio;
    private Date fechaFin;

    private List<CargoPago> cargos;
}
