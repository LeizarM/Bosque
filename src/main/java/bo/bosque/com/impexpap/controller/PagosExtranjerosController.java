package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.dto.FiltroFechasDto;
import bo.bosque.com.impexpap.dto.SolicitudPagoDto;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pagos-extranjeros")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class PagosExtranjerosController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    // ── DAOs originales ──────────────────────────────────────────────────────
    private final ISolicitudPago solicitudPagoDao;
    private final ISolicitudProveedor solicitudProveedorDao;
    private final IDetalleSolicitud detalleSolicitudDao;

    // ── DAOs nuevos ──────────────────────────────────────────────────────────
    private final ICanalesPago canalesPagoDao;
    private final IMonedas monedasDao;
    private final ITiposCambio tiposCambioDao;
    private final ITiposCargo tiposCargoDao;
    private final ITiposTransaccion tiposTransaccionDao;
    private final ITransacciones transaccionesDao;
    private final ILogEstados logEstadosDao;
    private final ICotizaciones cotizacionesDao;
    private final IConfigComisionesBanco configComisionesBancoDao;
    private final ICargoPago cargoPagoDao;

    public PagosExtranjerosController(
            ISolicitudPago solicitudPagoDao,
            ISolicitudProveedor solicitudProveedorDao,
            IDetalleSolicitud detalleSolicitudDao,
            ICanalesPago canalesPagoDao,
            IMonedas monedasDao,
            ITiposCambio tiposCambioDao,
            ITiposCargo tiposCargoDao,
            ITiposTransaccion tiposTransaccionDao,
            ITransacciones transaccionesDao,
            ILogEstados logEstadosDao,
            ICotizaciones cotizacionesDao,
            IConfigComisionesBanco configComisionesBancoDao,
            ICargoPago cargoPagoDao) {
        this.solicitudPagoDao = solicitudPagoDao;
        this.solicitudProveedorDao = solicitudProveedorDao;
        this.detalleSolicitudDao = detalleSolicitudDao;
        this.canalesPagoDao = canalesPagoDao;
        this.monedasDao = monedasDao;
        this.tiposCambioDao = tiposCambioDao;
        this.tiposCargoDao = tiposCargoDao;
        this.tiposTransaccionDao = tiposTransaccionDao;
        this.transaccionesDao = transaccionesDao;
        this.logEstadosDao = logEstadosDao;
        this.cotizacionesDao = cotizacionesDao;
        this.configComisionesBancoDao = configComisionesBancoDao;
        this.cargoPagoDao = cargoPagoDao;
    }

    // =========================================================================
    // ENDPOINTS DE REGISTRO / ACTUALIZACIÓN (Excepciones manejadas globalmente)
    // =========================================================================

    @PostMapping("/registrar-solicitud")
    public ResponseEntity<ApiResponse<?>> registrarSolicitud(@RequestBody SolicitudPago mb) {
        RespuestaSp res = solicitudPagoDao.registrarSolicitudPago(mb, mb.getIdSolicitud() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/registrar-solicitud-proveedor")
    public ResponseEntity<ApiResponse<?>> registrarSolicitudProveedor(@RequestBody SolicitudProveedor mb) {
        RespuestaSp res = solicitudProveedorDao.registrarSolicitudProveedor(mb, mb.getIdSolicitudProveedor() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/registrar-detalle-solicitud")
    public ResponseEntity<ApiResponse<?>> registrarDetalleSolicitud(@RequestBody DetalleSolicitud mb) {
        RespuestaSp res = detalleSolicitudDao.registrarDetalleSolicitud(mb, mb.getIdDetalle() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }


    @PostMapping("/guardar-completo")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardarSolicitudCompleta(@RequestBody SolicitudPago payload) {

        // ── 1. PROCESAR ELIMINACIONES PRIMERO (Para evitar conflictos FK) ──
        if (payload.getProveedores() != null) {
            for (SolicitudProveedor prov : payload.getProveedores()) {
                if (prov.getDetallesAEliminar() != null) {
                    for (Long idDetalleDel : prov.getDetallesAEliminar()) {
                        DetalleSolicitud detDel = new DetalleSolicitud();
                        detDel.setIdDetalle(idDetalleDel);
                        RespuestaSp resDel = detalleSolicitudDao.registrarDetalleSolicitud(detDel, "D");
                        if (resDel.getError() != 0) throw new RuntimeException("Error borrando factura: " + resDel.getErrormsg());
                    }
                }
            }
        }

        if (payload.getProveedoresAEliminar() != null) {
            for (Long idProvDel : payload.getProveedoresAEliminar()) {
                SolicitudProveedor provDel = new SolicitudProveedor();
                provDel.setIdSolicitudProveedor(idProvDel);
                RespuestaSp resDel = solicitudProveedorDao.registrarSolicitudProveedor(provDel, "D");
                if (resDel.getError() != 0) throw new RuntimeException("Error borrando proveedor: " + resDel.getErrormsg());
            }
        }

        // ── 2. REGISTRAR / ACTUALIZAR CABECERA (Solicitud) ──
        String accionSol = payload.getIdSolicitud() == 0 ? "I" : "U";
        RespuestaSp resSol = solicitudPagoDao.registrarSolicitudPago(payload, accionSol);

        if (resSol.getError() != 0) {
            // Activa el Rollback automático
            throw new RuntimeException("Error en Solicitud: " + resSol.getErrormsg());
        }

        long idSolicitud = (resSol.getIdGenerado() > 0) ? resSol.getIdGenerado() : payload.getIdSolicitud();

        // ── 3. REGISTRAR / ACTUALIZAR PROVEEDORES Y SUS DETALLES ──
        if (payload.getProveedores() != null) {
            for (SolicitudProveedor prov : payload.getProveedores()) {

                prov.setIdSolicitud(idSolicitud); // Asignar la llave foránea
                String accionProv = prov.getIdSolicitudProveedor() == 0 ? "I" : "U";
                RespuestaSp resProv = solicitudProveedorDao.registrarSolicitudProveedor(prov, accionProv);

                if (resProv.getError() != 0) {
                    throw new RuntimeException("Error en Proveedor " + prov.getCardCode() + ": " + resProv.getErrormsg());
                }

                long idProveedor = (resProv.getIdGenerado() > 0) ? resProv.getIdGenerado() : prov.getIdSolicitudProveedor();

                if (prov.getDetalles() != null) {
                    for (DetalleSolicitud det : prov.getDetalles()) {

                        det.setIdSolicitudProveedor(idProveedor); // Asignar la llave foránea
                        String accionDet = det.getIdDetalle() == 0 ? "I" : "U";
                        RespuestaSp resDet = detalleSolicitudDao.registrarDetalleSolicitud(det, accionDet);

                        if (resDet.getError() != 0) {
                            throw new RuntimeException("Error en Factura " + det.getNumeroDocumento() + ": " + resDet.getErrormsg());
                        }
                    }
                }
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(SUCCESS_MESSAGE, idSolicitud, HttpStatus.CREATED.value())
        );
    }

    // =========================================================================
    // ENDPOINTS DE OBTENCIÓN (LECTURA)
    // =========================================================================

    @PostMapping("/obtener")
    public ResponseEntity<ApiResponse<?>> obtenerSolicitud(@RequestBody SolicitudPago mb) {

        return procesarLista(solicitudPagoDao.obtenerSolicitudPago(mb.getIdSolicitud()), mb.getIdSolicitud());
    }

    @PostMapping("/obtener-solicitud-proveedor")
    public ResponseEntity<ApiResponse<?>> obtenerSolicitudProveedor(@RequestBody SolicitudProveedor mb) {

        return procesarLista(solicitudProveedorDao.obtenerSolicitudProveedor(mb.getIdSolicitudProveedor()), mb.getIdSolicitudProveedor());
    }

    @PostMapping("/obtener-detalle-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerDetalleSolicitud(@RequestBody DetalleSolicitud mb) {

        return procesarLista(detalleSolicitudDao.obtenerDetalleSolicitud(mb.getIdDetalle()), mb.getIdDetalle());
    }

    /**
     * Obtener los proveedores SAP filtrados por el código de Empresa
     */
    @PostMapping("/obtener-proveedores-empresa")
    public ResponseEntity<ApiResponse<?>> obtenerProveedoresXEmpresa( @RequestBody SolicitudProveedor mb ) {

        // 1. Ejecutamos el DAO pasándole el codEmpresa que viene en el JSON
        List<SolicitudProveedor> lista = this.solicitudProveedorDao.obtenerProveedoresXEmpresa( mb.getCodEmpresa() );

        // 2. Reutilizamos el método auxiliar
        return procesarLista( lista, mb.getCodEmpresa() );
    }


    /**
     * Obtener los las facturas proveedores y orden de compra por empresa
     */
    @PostMapping("/obtener-docnum-empresa")
    public ResponseEntity<ApiResponse<?>> obtenerFacProvYOrdCompraXEmpresa( @RequestBody DetalleSolicitud mb ) {


        List<DetalleSolicitud> lista = this.detalleSolicitudDao.obtenerFacProvYOrdCompraXEmpresa( mb.getCodEmpresa() );


        return procesarLista( lista, mb.getCodEmpresa() );
    }

    /**
     * Reporte jerárquico de solicitudes de pago filtradas por fechas
     */
    @PostMapping("/reporte-fechas")
    public ResponseEntity<ApiResponse<?>> reporteSolicitudesXFecha(@RequestBody FiltroFechasDto filtro) {

        List<SolicitudPagoDto> lista = solicitudPagoDao.reporteSolicitudesXFecha(
                filtro.getFechaInicio(),
                filtro.getFechaFin()
        );

        // Usamos el procesarLista genérico que armamos en la optimización
        return procesarLista(lista, 0L);
    }

    // =========================================================================
    // CANALES DE PAGO
    // =========================================================================

    @PostMapping("/registrar-canal-pago")
    public ResponseEntity<ApiResponse<?>> registrarCanalPago(@RequestBody CanalesPago mb) {
        RespuestaSp res = canalesPagoDao.registrarCanalesPago(mb, mb.getIdCanal() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/obtener-canales-pago")
    public ResponseEntity<ApiResponse<?>> obtenerCanalesPago(@RequestBody CanalesPago mb) {
        return procesarLista(canalesPagoDao.obtenerCanalesPago(mb.getIdCanal()), mb.getIdCanal());
    }

    // =========================================================================
    // MONEDAS
    // =========================================================================

    @PostMapping("/registrar-moneda")
    public ResponseEntity<ApiResponse<?>> registrarMoneda(@RequestBody Monedas mb) {
        RespuestaSp res = monedasDao.registrarMonedas(mb, mb.getIdMoneda() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/obtener-monedas")
    public ResponseEntity<ApiResponse<?>> obtenerMonedas(@RequestBody Monedas mb) {
        return procesarLista(monedasDao.obtenerMonedas(mb.getIdMoneda()), mb.getIdMoneda());
    }

    // =========================================================================
    // TIPOS DE CAMBIO
    // =========================================================================

    @PostMapping("/registrar-tipo-cambio")
    public ResponseEntity<ApiResponse<?>> registrarTipoCambio(@RequestBody TiposCambio mb) {
        RespuestaSp res = tiposCambioDao.registrarTiposCambio(mb, mb.getIdTipoCambio() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/obtener-tipos-cambio")
    public ResponseEntity<ApiResponse<?>> obtenerTiposCambio(@RequestBody TiposCambio mb) {
        return procesarLista(tiposCambioDao.obtenerTiposCambio(mb.getIdTipoCambio()), mb.getIdTipoCambio());
    }

    @PostMapping("/obtener-tipos-cambio-banco")
    public ResponseEntity<ApiResponse<?>> obtenerTiposCambioPorBanco(@RequestBody TiposCambio mb) {
        return procesarLista(tiposCambioDao.obtenerTiposCambioPorBanco(mb.getCodBanco()), mb.getCodBanco());
    }

    // =========================================================================
    // TIPOS DE CARGO
    // =========================================================================

    @PostMapping("/registrar-tipo-cargo")
    public ResponseEntity<ApiResponse<?>> registrarTipoCargo(@RequestBody TiposCargo mb) {
        RespuestaSp res = tiposCargoDao.registrarTiposCargo(mb, mb.getIdTipoCargo() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/obtener-tipos-cargo")
    public ResponseEntity<ApiResponse<?>> obtenerTiposCargo(@RequestBody TiposCargo mb) {
        return procesarLista(tiposCargoDao.obtenerTiposCargo(mb.getIdTipoCargo()), mb.getIdTipoCargo());
    }

    // =========================================================================
    // TIPOS DE TRANSACCIÓN
    // =========================================================================

    @PostMapping("/registrar-tipo-transaccion")
    public ResponseEntity<ApiResponse<?>> registrarTipoTransaccion(@RequestBody TiposTransaccion mb) {
        RespuestaSp res = tiposTransaccionDao.registrarTiposTransaccion(mb, mb.getIdTipoTransaccion() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/obtener-tipos-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerTiposTransaccion(@RequestBody TiposTransaccion mb) {
        return procesarLista(tiposTransaccionDao.obtenerTiposTransaccion(mb.getIdTipoTransaccion()), mb.getIdTipoTransaccion());
    }

    // =========================================================================
    // TRANSACCIONES
    // =========================================================================

    @PostMapping("/registrar-transaccion")
    public ResponseEntity<ApiResponse<?>> registrarTransaccion(@RequestBody Transacciones mb) {
        RespuestaSp res = transaccionesDao.registrarTransacciones(mb, mb.getIdTransaccion() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/obtener-transacciones")
    public ResponseEntity<ApiResponse<?>> obtenerTransacciones(@RequestBody Transacciones mb) {
        return procesarLista(transaccionesDao.obtenerTransacciones(mb.getIdTransaccion()), mb.getIdTransaccion());
    }

    @PostMapping("/obtener-transacciones-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccionesPorSolicitud(@RequestBody Transacciones mb) {
        return procesarLista(transaccionesDao.obtenerTransaccionesPorSolicitud(mb.getIdSolicitud()), mb.getIdSolicitud());
    }

    @PostMapping("/obtener-transacciones-cotizacion")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccionesPorCotizacion(@RequestBody Transacciones mb) {
        return procesarLista(transaccionesDao.obtenerTransaccionesPorCotizacion(mb.getIdCotizacion()), mb.getIdCotizacion());
    }

    // =========================================================================
    // LOG DE ESTADOS
    // =========================================================================

    @PostMapping("/registrar-log-estado")
    public ResponseEntity<ApiResponse<?>> registrarLogEstado(@RequestBody LogEstados mb) {
        RespuestaSp res = logEstadosDao.registrarLogEstados(mb, mb.getIdLog() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/obtener-log-estados")
    public ResponseEntity<ApiResponse<?>> obtenerLogEstados(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerLogEstados(mb.getIdLog()), mb.getIdLog());
    }

    @PostMapping("/obtener-log-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerLogPorSolicitud(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerLogPorSolicitud(mb.getIdSolicitud()), mb.getIdSolicitud());
    }

    @PostMapping("/obtener-log-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerLogPorTransaccion(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerLogPorTransaccion(mb.getIdTransaccion()), mb.getIdTransaccion());
    }

    // =========================================================================
    // COTIZACIONES
    // =========================================================================

    @PostMapping("/registrar-cotizacion")
    public ResponseEntity<ApiResponse<?>> registrarCotizacion(@RequestBody Cotizaciones mb) {
        RespuestaSp res = cotizacionesDao.registrarCotizaciones(mb, mb.getIdCotizacion() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/obtener-cotizaciones")
    public ResponseEntity<ApiResponse<?>> obtenerCotizaciones(@RequestBody Cotizaciones mb) {
        return procesarLista(cotizacionesDao.obtenerCotizaciones(mb.getIdCotizacion()), mb.getIdCotizacion());
    }

    @PostMapping("/obtener-cotizaciones-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerCotizacionesPorSolicitud(@RequestBody Cotizaciones mb) {
        return procesarLista(cotizacionesDao.obtenerCotizacionesPorSolicitud(mb.getIdSolicitud()), mb.getIdSolicitud());
    }

    // =========================================================================
    // CONFIGURACIÓN DE COMISIONES DE BANCO
    // =========================================================================

    @PostMapping("/registrar-config-comisiones")
    public ResponseEntity<ApiResponse<?>> registrarConfigComisiones(@RequestBody ConfigComisionesBanco mb) {
        RespuestaSp res = configComisionesBancoDao.registrarConfigComisionesBanco(mb, mb.getIdConfig() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/obtener-config-comisiones")
    public ResponseEntity<ApiResponse<?>> obtenerConfigComisiones(@RequestBody ConfigComisionesBanco mb) {
        return procesarLista(configComisionesBancoDao.obtenerConfigComisionesBanco(mb.getIdConfig()), mb.getIdConfig());
    }

    @PostMapping("/obtener-config-comisiones-banco")
    public ResponseEntity<ApiResponse<?>> obtenerConfigComisionesPorBanco(@RequestBody ConfigComisionesBanco mb) {
        return procesarLista(configComisionesBancoDao.obtenerConfigPorBanco(mb.getCodBanco()), mb.getCodBanco());
    }

    // =========================================================================
    // CARGOS DE PAGO
    // =========================================================================

    @PostMapping("/registrar-cargo-pago")
    public ResponseEntity<ApiResponse<?>> registrarCargoPago(@RequestBody CargoPago mb) {
        RespuestaSp res = cargoPagoDao.registrarCargoPago(mb, mb.getIdCargo() == 0 ? "I" : "U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/obtener-cargos-pago")
    public ResponseEntity<ApiResponse<?>> obtenerCargosPago(@RequestBody CargoPago mb) {
        return procesarLista(cargoPagoDao.obtenerCargoPago(mb.getIdCargo()), mb.getIdCargo());
    }

    @PostMapping("/obtener-cargos-cotizacion")
    public ResponseEntity<ApiResponse<?>> obtenerCargosPorCotizacion(@RequestBody CargoPago mb) {
        return procesarLista(cargoPagoDao.obtenerCargoPorCotizacion(mb.getIdCotizacion()), mb.getIdCotizacion());
    }

    @PostMapping("/obtener-cargos-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerCargosPorTransaccion(@RequestBody CargoPago mb) {
        return procesarLista(cargoPagoDao.obtenerCargoPorTransaccion(mb.getIdTransaccion()), mb.getIdTransaccion());
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    /**
     * Evalúa si la lista viene vacía para devolver 204 o 200.
     */
    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, long idBuscado) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("No se encontró el registro con ID: " + idBuscado, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}