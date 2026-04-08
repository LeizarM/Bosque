package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.service.FileStorageService;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.dto.ConfirmarPagoRequest;
import bo.bosque.com.impexpap.dto.FiltroFechasDto;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/pagos-extranjeros")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class PagosExtranjerosController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    /** Extensiones permitidas para vouchers. */
    private static final Set<String> EXTENSIONES_VOUCHER =
            new HashSet<>(Arrays.asList("pdf", "jpg", "jpeg", "png"));

    private final ISolicitudPago          solicitudPagoDao;
    private final ISolicitudProveedor     solicitudProveedorDao;
    private final IDetalleSolicitud       detalleSolicitudDao;
    private final ICanalesPago            canalesPagoDao;
    private final IMonedas                monedasDao;
    private final ITiposCambio            tiposCambioDao;
    private final ITiposCargo             tiposCargoDao;
    private final ITiposTransaccion       tiposTransaccionDao;
    private final ITransacciones          transaccionesDao;
    private final ILogEstados             logEstadosDao;
    private final ICotizaciones           cotizacionesDao;
    private final IConfigComisionesBanco  configComisionesBancoDao;
    private final ICargoPago              cargoPagoDao;
    private final FileStorageService      fileStorageService;


    public PagosExtranjerosController(
            ISolicitudPago         solicitudPagoDao,
            ISolicitudProveedor    solicitudProveedorDao,
            IDetalleSolicitud      detalleSolicitudDao,
            ICanalesPago           canalesPagoDao,
            IMonedas               monedasDao,
            ITiposCambio           tiposCambioDao,
            ITiposCargo            tiposCargoDao,
            ITiposTransaccion      tiposTransaccionDao,
            ITransacciones         transaccionesDao,
            ILogEstados            logEstadosDao,
            ICotizaciones          cotizacionesDao,
            IConfigComisionesBanco configComisionesBancoDao,
            ICargoPago             cargoPagoDao,
            FileStorageService     fileStorageService
            ) {
        this.solicitudPagoDao         = solicitudPagoDao;
        this.solicitudProveedorDao    = solicitudProveedorDao;
        this.detalleSolicitudDao      = detalleSolicitudDao;
        this.canalesPagoDao           = canalesPagoDao;
        this.monedasDao               = monedasDao;
        this.tiposCambioDao           = tiposCambioDao;
        this.tiposCargoDao            = tiposCargoDao;
        this.tiposTransaccionDao      = tiposTransaccionDao;
        this.transaccionesDao         = transaccionesDao;
        this.logEstadosDao            = logEstadosDao;
        this.cotizacionesDao          = cotizacionesDao;
        this.configComisionesBancoDao = configComisionesBancoDao;
        this.cargoPagoDao             = cargoPagoDao;
        this.fileStorageService       = fileStorageService;
    }

    // =========================================================================
    // FASE 1 — SOLICITUD COMPLETA (cabecera + proveedores + facturas)
    // ACID: si cualquier escritura falla → rollback de toda la jerarquía
    // =========================================================================

    /**
     * Crea o actualiza la solicitud completa en una sola transacción.
     *
     * JSON esperado:
     * {
     *   "idSolicitud": 0,              <- 0 = INSERT, >0 = UPDATE
     *   "codEmpresa": 1,
     *   "fechaSolicitud": "2026-03-18",
     *   "audUsuario": 5,
     *   "proveedoresAEliminar": [12, 15],
     *   "proveedores": [
     *     {
     *       "idSolicitudProveedor": 0,
     *       "cardCode": "VIZR829",
     *       "detallesAEliminar": [33],
     *       "detalles": [
     *         {
     *           "idDetalle": 0,
     *           "tipoDocumento": "FACTURA",
     *           "numeroDocumento": "F-001",
     *           "montoFacturaUsd": 15000.00,
     *           "montoAmortizadoUsd": 0,
     *           "fechaFactura": "2026-03-01",
     *           "fechaVencimiento": "2026-04-01"
     *         }
     *       ]
     *     }
     *   ]
     * }
     */
    @PostMapping("/guardar-solicitud-completa")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardarSolicitudCompleta(@RequestBody SolicitudPago payload) {

        // ── PASO 1: Eliminaciones primero (evitar violaciones FK) ─────────────
        if (payload.getProveedores() != null) {
            for (SolicitudProveedor prov : payload.getProveedores()) {
                if (prov.getDetallesAEliminar() != null) {
                    for (Long idDetalleDel : prov.getDetallesAEliminar()) {
                        DetalleSolicitud detDel = new DetalleSolicitud();
                        detDel.setIdDetalle(idDetalleDel);
                        detDel.setAudUsuario(payload.getAudUsuario());
                        ejecutar(detalleSolicitudDao.registrarDetalleSolicitud(detDel, "D"),
                                "Error eliminando factura ID=" + idDetalleDel);
                    }
                }
            }
        }

        if (payload.getProveedoresAEliminar() != null) {
            for (Long idProvDel : payload.getProveedoresAEliminar()) {
                SolicitudProveedor provDel = new SolicitudProveedor();
                provDel.setIdSolicitudProveedor(idProvDel);
                provDel.setAudUsuario(payload.getAudUsuario());
                ejecutar(solicitudProveedorDao.registrarSolicitudProveedor(provDel, "D"),
                        "Error eliminando proveedor ID=" + idProvDel);
            }
        }

        // ── PASO 2: Cabecera ──────────────────────────────────────────────────

        String accionSol  = payload.getIdSolicitud() == 0 ? "I" : "U";
        RespuestaSp resSol = solicitudPagoDao.registrarSolicitudPago(payload, accionSol);
        ejecutar(resSol, "Error en cabecera de solicitud");

        long idSolicitud = resSol.getIdGenerado() > 0 ? resSol.getIdGenerado() : payload.getIdSolicitud();


        // ── PASO 4: Proveedores y sus facturas ───────────────────────────────
        if (payload.getProveedores() != null) {
            for (SolicitudProveedor prov : payload.getProveedores()) {
                prov.setIdSolicitud(idSolicitud);
                // FIX: Garantizar que audUsuario se propague del padre si el hijo no lo trae
                if (prov.getAudUsuario() == 0) {
                    prov.setAudUsuario(payload.getAudUsuario());
                }
                String accionProv = prov.getIdSolicitudProveedor() == 0 ? "I" : "U";
                RespuestaSp resProv = solicitudProveedorDao.registrarSolicitudProveedor(prov, accionProv);
                ejecutar(resProv, "Error en proveedor " + prov.getCardCode());

                long idProveedor = resProv.getIdGenerado() > 0
                        ? resProv.getIdGenerado()
                        : prov.getIdSolicitudProveedor();

                if (prov.getDetalles() != null) {
                    for (DetalleSolicitud det : prov.getDetalles()) {
                        det.setIdSolicitudProveedor(idProveedor);
                        // FIX: Garantizar que audUsuario se propague del padre si el hijo no lo trae
                        if (det.getAudUsuario() == 0) {
                            det.setAudUsuario(payload.getAudUsuario());
                        }
                        String accionDet = det.getIdDetalle() == 0 ? "I" : "U";
                        RespuestaSp resDet = detalleSolicitudDao.registrarDetalleSolicitud(det, accionDet);
                        ejecutar(resDet, "Error en factura " + det.getNumeroDocumento());
                    }
                }
            }
        }

        return respuestaCreada(idSolicitud);
    }

    // =========================================================================
    // FASE 1 — CAMBIO DE ESTADO de la solicitud (PENDIENTE → APROBADA, etc.)
    // ACID: estado + log en la misma transacción
    // =========================================================================

    /**
     * Cambia el estado de la solicitud y registra el cambio en el log.
     * FIX: Se carga el registro actual antes del UPDATE para no enviar zeros/nulls
     *      que sobreescriban datos válidos (fechaSolicitud NOT NULL, codEmpresa, etc.).
     *
     * JSON esperado:
     * {
     *   "idSolicitud": 7,
     *   "estado": "APROBADA",
     *   "audUsuario": 3
     * }
     */
    @PostMapping("/aprobar-solicitud")
    @Transactional
    public ResponseEntity<ApiResponse<?>> aprobarSolicitud(@RequestBody SolicitudPago payload) {

        // ── PASO 1: Cargar registro actual para no perder datos en el UPDATE ──
        SolicitudPago actual = solicitudPagoDao.obtenerSolicitudPagoPorId(payload.getIdSolicitud());
        if (actual == null) {
            throw new SpBusinessException("No se encontró la solicitud con ID: " + payload.getIdSolicitud());
        }

        // Merge: solo actualizar el estado y auditoría, conservar el resto
        actual.setEstado(payload.getEstado());
        actual.setAudUsuario(payload.getAudUsuario());

        // ── PASO 2: Actualizar estado con datos completos ─────────────────────
        RespuestaSp res = solicitudPagoDao.registrarSolicitudPago(actual, "U");
        ejecutar(res, "Error cambiando estado de solicitud");


        return respuestaCreada(payload.getIdSolicitud());
    }

    // =========================================================================
    // FASE 2 — COTIZACIÓN COMPLETA (cotización + sus cargos)
    // ACID: si un cargo falla → rollback de la cotización y cargos anteriores
    // =========================================================================

    /**
     * Registra una cotización de banco con todos sus cargos.
     *
     * JSON esperado (campos mínimos requeridos para INSERT):
     * {
     *   "idCotizacion": 0,
     *   "idSolicitud": 7,
     *   "fechaCotizacion": "2026-03-18",
     *   "codBanco": 3,
     *   "montoCompra": 50000.00,
     *   "idMoneda": 1,
     *   "nroGiros": 1,
     *   "tipoCambioOfrecido": 6.97,
     *   "audUsuario": 5,
     *   "cargos": [
     *     { "idTipoCargo": 1, "porcentaje": 0.30, "baseCalculo": 348500.00, "montoCargo": 1045.50, "origenBase": "MONTO_CONVERTIDO", "idMoneda": 2, "orden": 1 },
     *     { "idTipoCargo": 2, "valorFijo": 150.00, "baseCalculo": 150.00,   "montoCargo": 150.00,  "origenBase": "MONTO_CONVERTIDO", "idMoneda": 2, "orden": 2 }
     *   ]
     * }
     */
    @PostMapping("/guardar-cotizacion-completa")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardarCotizacionCompleta(@RequestBody Cotizaciones payload) {

        // ── PASO 1: Cabecera de la cotización ────────────────────────────────
        boolean esNueva  = payload.getIdCotizacion() == 0;
        String accion    = esNueva ? "I" : "U";
        RespuestaSp res  = cotizacionesDao.registrarCotizaciones(payload, accion);
        ejecutar(res, "Error registrando cotización");

        long idCotizacion = res.getIdGenerado() > 0 ? res.getIdGenerado() : payload.getIdCotizacion();

        // ── PASO 2: Cargos de la cotización ──────────────────────────────────
        if ( esNueva && payload.getCargos() != null ) {
            int ordenAuto = 1;
            for (CargoPago cargo : payload.getCargos()) {
                cargo.setIdCotizacion(idCotizacion);
                cargo.setIdTransaccion(0L);             // exclusividad FK: no es cargo de transacción
                cargo.setAudUsuario(payload.getAudUsuario());
                // FIX: Garantizar orden secuencial si el frontend no lo envía
                if (cargo.getOrden() == 0) {
                    cargo.setOrden(ordenAuto);
                }
                ordenAuto++;
                RespuestaSp resCargo = cargoPagoDao.registrarCargoPago(cargo, "I");
                ejecutar(resCargo, "Error registrando cargo en cotización");
            }
        }

        return respuestaCreada(idCotizacion);
    }

    // =========================================================================
    // FASE 3 — ACEPTAR COTIZACIÓN GANADORA
    // ACID: marcar ganadora + rechazar las demás + log en una sola TX
    // (el SP ya hace el rechazo masivo en una TX interna,
    //  @Transactional de Java agrega el log al mismo bloque)
    // =========================================================================

    /**
     * Acepta la cotización ganadora. El SP rechaza automáticamente las demás.
     *
     * JSON esperado:
     * {
     *   "idCotizacion": 12,
     *   "estado": "ACEPTADA",
     *   "audUsuario": 5
     * }
     */
    @PostMapping("/aceptar-cotizacion")
    @Transactional
    public ResponseEntity<ApiResponse<?>> aceptarCotizacion(@RequestBody Cotizaciones payload) {

        RespuestaSp res = cotizacionesDao.registrarCotizaciones(payload, "U");
        ejecutar(res, "Error aceptando cotización");

        return respuestaCreada(payload.getIdCotizacion());
    }

    // =========================================================================
    // FASE 4 — TRANSACCIÓN COMPLETA (transacción + cargos + log)
    // ACID: si cualquier cargo falla → rollback de la transacción y cargos
    // =========================================================================

    /**
     * Registra la transacción de pago con todos sus cargos en una sola TX.
     *
     * JSON esperado (campos mínimos para INSERT):
     * {
     *   "idTransaccion": 0,
     *   "idSolicitud": 7,
     *   "idCotizacion": 12,
     *   "idTipoTransaccion": 2,
     *   "codBanco": 3,
     *   "codEmpresa": 1,
     *   "cardCode": "VIZR829",
     *   "fechaTransaccion": "2026-03-18",
     *   "montoOrigen": 50000.00,
     *   "idMonedaOrigen": 1,
     *   "tipoCambioAplicado": 6.97,
     *   "montoConvertido": 348500.00,
     *   "idMonedaDestino": 2,
     *   "tipoCambioReferencia": 6.86,
     *   "totalFinal": 348500.00,
     *   "audUsuario": 5,
     *   "cargos": [
     *     { "idTipoCargo": 1, "porcentaje": 0.30, "baseCalculo": 348500.00, "montoCargo": 1045.50, "origenBase": "MONTO_CONVERTIDO", "idMoneda": 2, "orden": 1 },
     *     { "idTipoCargo": 3, "valorFijo": 200.00, "baseCalculo": 200.00,   "montoCargo": 200.00,  "origenBase": "MONTO_CONVERTIDO", "idMoneda": 2, "orden": 2 }
     *   ]
     * }
     */
    @PostMapping("/guardar-transaccion-completa")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardarTransaccionCompleta(@RequestBody Transacciones payload) {


        String accion   = payload.getIdTransaccion() == 0 ? "I" : "U";
        RespuestaSp res = transaccionesDao.registrarTransacciones(payload, accion);
        ejecutar(res, "Error registrando transacción");

        long idTransaccion = res.getIdGenerado() > 0 ? res.getIdGenerado() : payload.getIdTransaccion();

        if ( payload.getIdTransaccion() == 0 && payload.getCargos() != null) {
            int ordenAuto = 1;
            for (CargoPago cargo : payload.getCargos()) {
                cargo.setIdTransaccion(idTransaccion);
                cargo.setIdCotizacion(0L);              // exclusividad FK: no es cargo de cotización
                cargo.setAudUsuario(payload.getAudUsuario());
                // FIX: Garantizar orden secuencial si el frontend no lo envía
                if (cargo.getOrden() == 0) {
                    cargo.setOrden(ordenAuto);
                }
                ordenAuto++;
                RespuestaSp resCargo = cargoPagoDao.registrarCargoPago(cargo, "I");
                ejecutar(resCargo, "Error registrando cargo en transacción");
            }
        }


        return respuestaCreada(idTransaccion);
    }

    // =========================================================================
    // FASE 4 — CAMBIO DE ESTADO de la transacción (PENDIENTE → PROCESADO, etc.)
    // ACID: estado + log en la misma transacción Java
    // =========================================================================

    /**
     * Cambia el estado de la transacción y registra el log.
     * FIX: Se carga el registro actual antes del UPDATE para no enviar zeros/nulls
     *      que sobreescriban campos NOT NULL de la tabla.
     *
     * JSON esperado:
     * {
     *   "idTransaccion": 9,
     *   "estado": "PROCESADO",
     *   "numeroTransaccion": "OP-2026-001",
     *   "fechaValor": "2026-03-18",
     *   "codEmpresa": 1,
     *   "audUsuario": 5
     * }
     */
    @PostMapping("/cambiar-estado-transaccion")
    @Transactional
    public ResponseEntity<ApiResponse<?>> cambiarEstadoTransaccion(@RequestBody Transacciones payload) {

        // ── PASO 1: Cargar registro actual para no perder datos en el UPDATE ──
        Transacciones actual = transaccionesDao.obtenerTransaccionPorId(payload.getIdTransaccion(), payload.getCodEmpresa());
        if (actual == null) {
            throw new SpBusinessException("No se encontró la transacción con ID: " + payload.getIdTransaccion());
        }

        // Merge: solo actualizar los campos de estado, conservar el resto
        actual.setEstado(payload.getEstado());
        actual.setAudUsuario(payload.getAudUsuario());
        if (payload.getNumeroTransaccion() != null) {
            actual.setNumeroTransaccion(payload.getNumeroTransaccion());
        }
        if (payload.getFechaValor() != null) {
            actual.setFechaValor(payload.getFechaValor());
        }

        // ── PASO 2: Actualizar con datos completos ────────────────────────────
        RespuestaSp res = transaccionesDao.registrarTransacciones(actual, "U");
        ejecutar(res, "Error cambiando estado de transacción");



        return respuestaCreada(payload.getIdTransaccion());
    }

    // =========================================================================
    // FASE 5 — CONFIRMAR Y CERRAR (transacción CONFIRMADA + solicitud PAGADA)
    // ACID: ambas entidades y sus logs en una sola transacción Java
    // =========================================================================

    /**
     * Confirma el débito bancario y cierra la solicitud en una sola TX.
     * FIX: Se cargan los registros actuales antes de cada UPDATE para no perder datos.
     *
     * JSON esperado:
     * {
     *   "idTransaccion": 9,
     *   "idSolicitud": 7,
     *   "codEmpresa": 1,
     *   "numeroTransaccion": "OP-2026-001",
     *   "fechaValor": "2026-03-18",
     *   "audUsuario": 5
     * }
     */
    @PostMapping("/confirmar-pago")
    @Transactional
    public ResponseEntity<ApiResponse<?>> confirmarPago(@RequestBody ConfirmarPagoRequest payload) {

        // ── PASO 1: Cargar transacción actual y hacer merge → CONFIRMADO ─────
        Transacciones trxActual = transaccionesDao.obtenerTransaccionPorId(payload.getIdTransaccion(), payload.getCodEmpresa());
        if (trxActual == null) {
            throw new SpBusinessException("No se encontró la transacción con ID: " + payload.getIdTransaccion());
        }
        trxActual.setEstado("CONFIRMADO");
        trxActual.setAudUsuario(payload.getAudUsuario());
        if (payload.getNumeroTransaccion() != null) {
            trxActual.setNumeroTransaccion(payload.getNumeroTransaccion());
        }
        if (payload.getFechaValor() != null) {
            trxActual.setFechaValor(payload.getFechaValor());
        }

        RespuestaSp resTrx = transaccionesDao.registrarTransacciones(trxActual, "U");
        ejecutar(resTrx, "Error confirmando transacción");



        // ── PASO 2: Cargar solicitud actual y hacer merge → PAGADA ──────────
        SolicitudPago solActual = solicitudPagoDao.obtenerSolicitudPagoPorId(payload.getIdSolicitud());
        if (solActual == null) {
            throw new SpBusinessException("No se encontró la solicitud con ID: " + payload.getIdSolicitud());
        }
        solActual.setEstado("PAGADA");
        solActual.setAudUsuario((int) payload.getAudUsuario());
        // Si el frontend envía montoTotalSolicitud actualizado, usarlo
        if (payload.getMontoTotalSolicitud() > 0) {
            solActual.setMontoTotalSolicitud(payload.getMontoTotalSolicitud());
        }

        RespuestaSp resSol = solicitudPagoDao.registrarSolicitudPago(solActual, "U");
        ejecutar(resSol, "Error cerrando solicitud como PAGADA");


        return respuestaCreada(payload.getIdSolicitud());
    }

    // =========================================================================
    // ENDPOINTS DE LECTURA — Sin @Transactional (solo SELECT)
    // =========================================================================

    @PostMapping("/obtener-solicitudes")
    public ResponseEntity<ApiResponse<?>> obtenerSolicitudes(@RequestBody SolicitudPago mb) {
        return procesarLista(solicitudPagoDao.obtenerSolicitudPago(mb.getIdSolicitud()),
                "No se encontraron solicitudes.");
    }

    @PostMapping("/obtener-solicitud-proveedor")
    public ResponseEntity<ApiResponse<?>> obtenerSolicitudProveedor(@RequestBody SolicitudProveedor mb) {
        return procesarLista(solicitudProveedorDao.obtenerSolicitudProveedor(mb.getIdSolicitudProveedor()),
                "No se encontró el proveedor con ID: " + mb.getIdSolicitudProveedor());
    }

    @PostMapping("/obtener-detalle-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerDetalleSolicitud(@RequestBody DetalleSolicitud mb) {
        return procesarLista(detalleSolicitudDao.obtenerDetalleSolicitud(mb.getIdDetalle()),
                "No se encontró el detalle con ID: " + mb.getIdDetalle());
    }

    @PostMapping("/obtener-proveedores-empresa")
    public ResponseEntity<ApiResponse<?>> obtenerProveedoresXEmpresa(@RequestBody SolicitudProveedor mb) {
        return procesarLista(solicitudProveedorDao.obtenerProveedoresXEmpresa(mb.getCodEmpresa()),
                "No se encontraron proveedores para la empresa indicada.");
    }

    @PostMapping("/obtener-docnum-empresa")
    public ResponseEntity<ApiResponse<?>> obtenerDocumentosPorEmpresa(@RequestBody DetalleSolicitud mb) {
        return procesarLista(detalleSolicitudDao.obtenerFacProvYOrdCompraXEmpresa(mb.getCodEmpresa()),
                "No se encontraron documentos para la empresa indicada.");
    }

    @PostMapping("/reporte-solicitudes-fechas")
    public ResponseEntity<ApiResponse<?>> reporteSolicitudesXFecha(@RequestBody FiltroFechasDto filtro) {
        return procesarLista(solicitudPagoDao.reporteSolicitudesXFecha(filtro.getFechaInicio(), filtro.getFechaFin()),
                "No se encontraron solicitudes en el rango indicado.");
    }

    @PostMapping("/obtener-cotizaciones-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerCotizacionesPorSolicitud(@RequestBody Cotizaciones mb) {
        return procesarLista(cotizacionesDao.obtenerCotizacionesPorSolicitud(mb.getIdSolicitud()),
                "No se encontraron cotizaciones para la solicitud: " + mb.getIdSolicitud());
    }

    /**
     * [L] Grilla de transacciones de una solicitud.
     * Body: { idSolicitud, cardCode, codBanco, estado, idTipoTransaccion, codEmpresa }
     */
    @PostMapping("/obtener-transacciones-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccionesPorSolicitud(@RequestBody Transacciones mb) {
        return procesarLista(
                transaccionesDao.obtenerTransacciones(
                        mb.getIdSolicitud(), mb.getCardCode(), mb.getCodBanco(),
                        mb.getEstado(), mb.getIdTipoTransaccion(), mb.getCodEmpresa()),
                "No se encontraron transacciones para la solicitud: " + mb.getIdSolicitud());
    }

    /**
     * [C] Transacciones vinculadas a una cotización específica.
     * Body: { idCotizacion }
     */
    @PostMapping("/obtener-transacciones-cotizacion")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccionesPorCotizacion(@RequestBody Transacciones mb) {
        return procesarLista(
                transaccionesDao.obtenerTransaccionesPorCotizacion(mb.getIdCotizacion()),
                "No se encontraron transacciones para la cotización: " + mb.getIdCotizacion());
    }

    /**
     * [R] Registro completo de una transacción (para formulario).
     * Body: { idTransaccion, codEmpresa }
     */
    @PostMapping("/obtener-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccion(@RequestBody Transacciones mb) {
        Transacciones result = transaccionesDao.obtenerTransaccion(mb.getIdTransaccion(), mb.getCodEmpresa());
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("No se encontró la transacción: " + mb.getIdTransaccion(),
                            null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, result, HttpStatus.OK.value()));
    }

    /**
     * [B] Reporte de transacciones entre fechas.
     * Body: { fechaInicio, fechaFin, cardCode, estado, idTipoTransaccion, codEmpresa }
     */
    @PostMapping("/reporte-transacciones-fechas")
    public ResponseEntity<ApiResponse<?>> reporteTransaccionesXFecha(@RequestBody Transacciones mb) {
        return procesarLista(
                transaccionesDao.obtenerTransaccionesReporte(
                        mb.getFechaInicio(), mb.getFechaFin(), mb.getCardCode(),
                        mb.getEstado(), mb.getIdTipoTransaccion(), mb.getCodEmpresa()),
                "No se encontraron transacciones en el rango indicado.");
    }

    @PostMapping("/obtener-cargos-cotizacion")
    public ResponseEntity<ApiResponse<?>> obtenerCargosPorCotizacion(@RequestBody CargoPago mb) {
        return procesarLista(cargoPagoDao.obtenerCargoPorCotizacion(mb.getIdCotizacion()),
                "No se encontraron cargos para la cotización: " + mb.getIdCotizacion());
    }

    @PostMapping("/obtener-cargos-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerCargosPorTransaccion(@RequestBody CargoPago mb) {
        return procesarLista(cargoPagoDao.obtenerCargoPorTransaccion(mb.getIdTransaccion()),
                "No se encontraron cargos para la transacción: " + mb.getIdTransaccion());
    }

    @PostMapping("/obtener-log-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerLogPorSolicitud(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerLogPorSolicitud(mb.getIdSolicitud()),
                "No se encontraron logs para la solicitud: " + mb.getIdSolicitud());
    }

    @PostMapping("/obtener-log-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerLogPorTransaccion(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerLogPorTransaccion(mb.getIdTransaccion()),
                "No se encontraron logs para la transacción: " + mb.getIdTransaccion());
    }

    /**
     * [F] Timeline completo de una solicitud: estados de la solicitud,
     * sus cotizaciones y sus transacciones en orden cronológico.
     * Body: { "idSolicitud": 7 }
     */
    @PostMapping("/obtener-timeline-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerTimelineSolicitud(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerTimelineCompleto(mb.getIdSolicitud()),
                "No se encontraron eventos en el timeline de la solicitud: " + mb.getIdSolicitud());
    }



    // =========================================================================
    // ENDPOINTS DE CATÁLOGOS — Registrar / Eliminar / Obtener
    // =========================================================================

    @PostMapping("/registrar-canal-pago")
    public ResponseEntity<ApiResponse<?>> registrarCanalPago(@RequestBody CanalesPago mb) {
        return respuestaEscritura(canalesPagoDao.registrarCanalesPago(mb, mb.getIdCanal() == 0 ? "I" : "U"));
    }
    @PostMapping("/eliminar-canal-pago")
    public ResponseEntity<ApiResponse<?>> eliminarCanalPago(@RequestBody CanalesPago mb) {
        return respuestaEscritura(canalesPagoDao.registrarCanalesPago(mb, "D"));
    }
    @PostMapping("/obtener-canales-pago")
    public ResponseEntity<ApiResponse<?>> obtenerCanalesPago(@RequestBody CanalesPago mb) {
        return procesarLista(canalesPagoDao.obtenerCanalesPago(mb.getIdCanal()), "No se encontraron canales de pago.");
    }

    @PostMapping("/registrar-moneda")
    public ResponseEntity<ApiResponse<?>> registrarMoneda(@RequestBody Monedas mb) {
        return respuestaEscritura(monedasDao.registrarMonedas(mb, mb.getIdMoneda() == 0 ? "I" : "U"));
    }
    @PostMapping("/eliminar-moneda")
    public ResponseEntity<ApiResponse<?>> eliminarMoneda(@RequestBody Monedas mb) {
        return respuestaEscritura(monedasDao.registrarMonedas(mb, "D"));
    }
    @PostMapping("/obtener-monedas")
    public ResponseEntity<ApiResponse<?>> obtenerMonedas(@RequestBody Monedas mb) {
        return procesarLista(monedasDao.obtenerMonedas(mb.getIdMoneda()), "No se encontraron monedas.");
    }

    @PostMapping("/registrar-tipo-cambio")
    public ResponseEntity<ApiResponse<?>> registrarTipoCambio(@RequestBody TiposCambio mb) {
        return respuestaEscritura(tiposCambioDao.registrarTiposCambio(mb, mb.getIdTipoCambio() == 0 ? "I" : "U"));
    }
    @PostMapping("/eliminar-tipo-cambio")
    public ResponseEntity<ApiResponse<?>> eliminarTipoCambio(@RequestBody TiposCambio mb) {
        return respuestaEscritura(tiposCambioDao.registrarTiposCambio(mb, "D"));
    }
    @PostMapping("/obtener-tipos-cambio")
    public ResponseEntity<ApiResponse<?>> obtenerTiposCambio(@RequestBody TiposCambio mb) {
        return procesarLista(tiposCambioDao.obtenerTiposCambio(mb.getIdTipoCambio()), "No se encontraron tipos de cambio.");
    }
    @PostMapping("/obtener-tipos-cambio-banco")
    public ResponseEntity<ApiResponse<?>> obtenerTiposCambioPorBanco(@RequestBody TiposCambio mb) {
        return procesarLista(tiposCambioDao.obtenerTiposCambioPorBanco(mb.getCodBanco()),
                "No se encontraron tipos de cambio para el banco: " + mb.getCodBanco());
    }

    @PostMapping("/registrar-tipo-cargo")
    public ResponseEntity<ApiResponse<?>> registrarTipoCargo(@RequestBody TiposCargo mb) {
        return respuestaEscritura(tiposCargoDao.registrarTiposCargo(mb, mb.getIdTipoCargo() == 0 ? "I" : "U"));
    }
    @PostMapping("/eliminar-tipo-cargo")
    public ResponseEntity<ApiResponse<?>> eliminarTipoCargo(@RequestBody TiposCargo mb) {
        return respuestaEscritura(tiposCargoDao.registrarTiposCargo(mb, "D"));
    }
    @PostMapping("/obtener-tipos-cargo")
    public ResponseEntity<ApiResponse<?>> obtenerTiposCargo(@RequestBody TiposCargo mb) {
        return procesarLista(tiposCargoDao.obtenerTiposCargo(mb.getIdTipoCargo()), "No se encontraron tipos de cargo.");
    }

    @PostMapping("/registrar-tipo-transaccion")
    public ResponseEntity<ApiResponse<?>> registrarTipoTransaccion(@RequestBody TiposTransaccion mb) {
        return respuestaEscritura(tiposTransaccionDao.registrarTiposTransaccion(mb, mb.getIdTipoTransaccion() == 0 ? "I" : "U"));
    }
    @PostMapping("/eliminar-tipo-transaccion")
    public ResponseEntity<ApiResponse<?>> eliminarTipoTransaccion(@RequestBody TiposTransaccion mb) {
        return respuestaEscritura(tiposTransaccionDao.registrarTiposTransaccion(mb, "D"));
    }
    @PostMapping("/obtener-tipos-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerTiposTransaccion(@RequestBody TiposTransaccion mb) {
        return procesarLista(tiposTransaccionDao.obtenerTiposTransaccion(mb.getIdTipoTransaccion()),
                "No se encontraron tipos de transacción.");
    }

    @PostMapping("/registrar-config-comisiones")
    public ResponseEntity<ApiResponse<?>> registrarConfigComisiones(@RequestBody ConfigComisionesBanco mb) {
        return respuestaEscritura(configComisionesBancoDao.registrarConfigComisionesBanco(mb, mb.getIdConfig() == 0 ? "I" : "U"));
    }
    @PostMapping("/eliminar-config-comisiones")
    public ResponseEntity<ApiResponse<?>> eliminarConfigComisiones(@RequestBody ConfigComisionesBanco mb) {
        return respuestaEscritura(configComisionesBancoDao.registrarConfigComisionesBanco(mb, "D"));
    }
    @PostMapping("/obtener-config-comisiones")
    public ResponseEntity<ApiResponse<?>> obtenerConfigComisiones(@RequestBody ConfigComisionesBanco mb) {
        return procesarLista(configComisionesBancoDao.obtenerConfigComisionesBanco(mb.getIdConfig()),
                "No se encontró la configuración con ID: " + mb.getIdConfig());
    }
    @PostMapping("/obtener-config-comisiones-banco")
    public ResponseEntity<ApiResponse<?>> obtenerConfigComisionesPorBanco(@RequestBody ConfigComisionesBanco mb) {
        return procesarLista(configComisionesBancoDao.obtenerConfigPorBanco(mb.getCodBanco()),
                "No se encontraron configuraciones para el banco: " + mb.getCodBanco());
    }

    // =========================================================================
    // VOUCHERS DE TRANSACCIONES — Subir y obtener archivo voucher
    // =========================================================================

    /**
     * Sube el voucher de una transacción (PDF, JPG, JPEG o PNG).
     * <p>
     * Flujo:
     * 1. Valida que el archivo no sea nulo ni vacío.
     * 2. Valida la extensión (solo PDF, JPG, JPEG, PNG).
     * 3. Construye la ruta relativa bajo uploads/.
     * 4. Guarda el archivo en disco.
     * 5. Actualiza rutaVoucher en BD (solo ese campo).
     * 6. Si el SP devuelve error, elimina el archivo como rollback.
     *
     * Parámetros multipart:
     *   file        — archivo a subir
     *   audUsuario  — ID del usuario que realiza la operación
     */
    @PostMapping(value = "/transacciones/{idTransaccion}/voucher", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> subirVoucher(
            @PathVariable long idTransaccion,
            @RequestParam("file") MultipartFile file,
            @RequestParam("audUsuario") int audUsuario) {

        // 1. Validar que el archivo no sea nulo ni vacío
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("El archivo no puede estar vacío.", null, HttpStatus.BAD_REQUEST.value()));
        }

        // 2. Validar extensión: solo PDF, JPG, JPEG, PNG
        String originalFilename = Objects.requireNonNull(
                file.getOriginalFilename(), "Nombre de archivo no disponible");
        String ext = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!EXTENSIONES_VOUCHER.contains(ext)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Extensión no permitida. Use PDF, JPG, JPEG o PNG.",
                            null, HttpStatus.BAD_REQUEST.value()));
        }

        // 3. Construir ruta relativa
        String rutaRelativa = "pagos-extranjeros/vouchers/"
                + idTransaccion + "_" + System.currentTimeMillis() + "." + ext;

        try {
            // 4. Guardar el archivo en disco
            fileStorageService.guardarVoucher(file, rutaRelativa);

            // 5. Actualizar la BD (SpHelper lanza SpBusinessException si error != 0)
            try {
                transaccionesDao.actualizarVoucher(idTransaccion, rutaRelativa, audUsuario);
            } catch (SpBusinessException spEx) {
                // 6. Rollback del archivo si el SP rechaza la operación
                fileStorageService.eliminarVoucher(rutaRelativa);
                log.warn("Voucher eliminado por error en SP. Transacción ID={}: {}", idTransaccion, spEx.getMessage());
                throw spEx; // GlobalExceptionHandler devuelve 400
            }

            // 7. Respuesta exitosa con la ruta guardada
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, rutaRelativa, HttpStatus.CREATED.value()));

        } catch (SpBusinessException ex) {
            throw ex;
        } catch (IOException e) {
            log.error("Error guardando voucher para transacción ID={}: {}", idTransaccion, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Error al guardar el archivo.", null,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    /**
     * Devuelve el voucher de una transacción como stream para visualización o descarga.
     * GET /pagos-extranjeros/transacciones/{idTransaccion}/voucher?codEmpresa=1
     */
    @GetMapping("/transacciones/{idTransaccion}/voucher")
    public ResponseEntity<?> obtenerVoucher(
            @PathVariable long idTransaccion,
            @RequestParam(value = "codEmpresa", required = false, defaultValue = "0") long codEmpresa) {

        // 1. Obtener la transacción
        Transacciones trx = transaccionesDao.obtenerTransaccionPorId(idTransaccion, codEmpresa);


        // 2. Verificar si tiene voucher registrado
        if (trx == null || trx.getRutaVoucher() == null
                || Boolean.FALSE.equals(trx.getTieneVoucher())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>("La transacción no tiene voucher registrado.",
                            null, HttpStatus.NOT_FOUND.value()));
        }

        try {
            // 3. Cargar el archivo desde disco
            Resource resource = fileStorageService.obtenerVoucher(trx.getRutaVoucher());

            // 4. Detectar ContentType por extensión (no usar Files.probeContentType:
            //    en Windows devuelve null para PDF y otros tipos, causando octet-stream)
            String rutaVoucher = trx.getRutaVoucher();
            String ext = rutaVoucher.contains(".")
                    ? rutaVoucher.substring(rutaVoucher.lastIndexOf('.') + 1).toLowerCase()
                    : "";
            String contentType;
            switch (ext) {
                case "pdf":          contentType = "application/pdf";       break;
                case "jpg":
                case "jpeg":         contentType = "image/jpeg";            break;
                case "png":          contentType = "image/png";             break;
                default:             contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            // 5. Retornar el archivo como stream (inline para visualización en browser)
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(resource.contentLength())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("Error leyendo voucher para transacción ID={}: {}", idTransaccion, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Error al leer el archivo.", null,
                            HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }


    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    /**
     * FIX: Lanza SpBusinessException (→ HTTP 400 via GlobalExceptionHandler)
     * en vez de RuntimeException genérica (que producía HTTP 500).
     * Dentro de un @Transactional, esto dispara el rollback automático.
     *
     * Nota: SpHelper ya lanza SpBusinessException para error != 0,
     * pero este método es defensa adicional por si la implementación cambia.
     */
    private void ejecutar(RespuestaSp res, String contexto) {
        if (res.getError() != 0) {
            throw new SpBusinessException(contexto + ": " + res.getErrormsg());
        }
    }

    /**
     * Registra un cambio de estado en tpex_LogEstados.
     * Exactamente uno de los tres IDs debe ser != null.
     *
     * FIX: audUsuario cambiado a long para coincidir con LogEstados.audUsuario (bigint en BD).
     * FIX: Lanza SpBusinessException en vez de RuntimeException.
     */
    private void registrarLog(Long idSolicitud, Long idCotizacion, Long idTransaccion,
                              String estadoNuevo, long audUsuario) {
        LogEstados log = new LogEstados();
        // FIX: se pasan como null (no como 0L) para que el SP pueda evaluar
        // correctamente IS NOT NULL al clasificar tipoEntidad (SOLICITUD/COTIZACION/TRANSACCION).
        log.setIdSolicitud(idSolicitud);
        log.setIdCotizacion(idCotizacion);
        log.setIdTransaccion(idTransaccion);
        log.setEstadoNuevo(estadoNuevo);
        log.setAudUsuario(audUsuario);
        RespuestaSp res = logEstadosDao.registrarLogEstados(log, "I");
        if (res.getError() != 0) {
            throw new SpBusinessException("Error registrando log de estado [" + estadoNuevo + "]: " + res.getErrormsg());
        }
    }

    /** Respuesta 201 para escrituras exitosas. */
    private ResponseEntity<ApiResponse<?>> respuestaCreada(long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, id, HttpStatus.CREATED.value()));
    }

    /** Respuesta para operaciones de catálogo (201 OK, 400 si SP devuelve error). */
    private ResponseEntity<ApiResponse<?>> respuestaEscritura(RespuestaSp res) {
        HttpStatus status = res.getError() == 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), status.value()));
    }

    /** 200 con datos o 204 si lista vacía, con mensaje personalizado. */
    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, String mensajeVacio) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}
