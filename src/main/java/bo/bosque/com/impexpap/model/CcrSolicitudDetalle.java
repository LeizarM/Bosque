package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Un item de una solicitud de corte (tccr_ccrSolicitudDetalle).
 *
 * Los campos vienen en tres bloques:
 *
 *  - **Base**: el articulo que entra a cortarse, copiado del catalogo SAP.
 *  - **Salida**: como sale. En las solicitudes ESPECIALES la salida es el mismo
 *    articulo que entra, asi que estos campos se copian de los Base — la tabla
 *    los exige NOT NULL. Lo que de verdad define el corte especial son
 *    anchoSalidaEsp, largoSalidaEsp, cantHojasSalidaEsp y nroCortes.
 *  - **sap***: los devuelve SAP cuando toma la solicitud. Son de solo lectura
 *    aqui; los actualiza la reconciliacion (p_abm_CcrSolicitudDetalle ACCION 'F').
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CcrSolicitudDetalle implements Serializable {

    private long idSolicitudDetalle;
    private long idSolicitud;

    // ── Articulo que entra ──────────────────────────────────────────────────
    private String codigoSAPBase;
    private String datoSAPBase;
    private double stockDisponibleSAPBase;
    private int codTipoItemSAPBase;
    private String datoTipoItemSAPBase;
    private int codFabricanteSAPBase;
    private String datoFabricanteSAPBase;
    private double gramajeSAPBase;
    private double largoSAPBase;
    private double anchoSAPBase;
    private double utmSAPBase;
    private String empaqueSAPBase;

    // ── Articulo que sale ───────────────────────────────────────────────────
    private String codigoSAPSalida;
    private String datoSAPSalida;
    private int codTipoItemSAPSalida;
    private String datoTipoItemSAPSalida;
    private int codFabricanteSAPSalida;
    private String datoFabricanteSAPSalida;
    private double gramajeSAPSalida;
    private double largoSAPSalida;
    private double anchoSAPSalida;
    private double utmSAPSalida;
    private double cantHojasSAPSalida;
    private String empaqueSAPSalida;

    // ── Lo que se pide ──────────────────────────────────────────────────────
    private double cantPaquetesSolicitados;
    private double cantToneladasSolicitados;
    private Date fechaEntrega;

    // ── El corte especial ───────────────────────────────────────────────────
    private double anchoSalidaEsp;
    private double largoSalidaEsp;
    private int cantHojasSalidaEsp;
    private int nroCortes;

    // ── Lo que devuelve SAP ─────────────────────────────────────────────────
    private long sapDocEntry;
    private long sapDocNum;
    private String sapItemCode;
    private String sapProdName;
    private String sapEstado;
    private double sapPlannedQty;
    private String sapComments;
    private String sapTipoCorte;
    private long sapU_nroCorte;
    private int sapCodEmpresa;
    private String sapDatoEmpresa;

    private long audUsuario;

    //===== ATRIBUTOS ADICIONALES (solo lectura, los resuelve el SP)
    private String datoFecCierreSistem;
    private String datoFecCierreStr;
    private String datoFecInicioStr;
    private String datoFechaEntregaStr;

}
