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

/**
 * Controlador REST para gestión de pagos extranjeros.
 */
@Slf4j
@RestController
@RequestMapping("/pagos-extranjeros")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class PagosExtranjerosController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";
    private static final Set<String> EXTENSIONES_VOUCHER = new HashSet<>(Arrays.asList("pdf", "jpg", "jpeg", "png"));

    private final ISolicitudPago solicitudPagoDao;
    private final ISolicitudProveedor solicitudProveedorDao;
    private final IDetalleSolicitud detalleSolicitudDao;
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
    private final FileStorageService fileStorageService;

    public PagosExtranjerosController(
            ISolicitudPago solicitudPagoDao, ISolicitudProveedor solicitudProveedorDao,
            IDetalleSolicitud detalleSolicitudDao, ICanalesPago canalesPagoDao,
            IMonedas monedasDao, ITiposCambio tiposCambioDao, ITiposCargo tiposCargoDao,
            ITiposTransaccion tiposTransaccionDao, ITransacciones transaccionesDao,
            ILogEstados logEstadosDao, ICotizaciones cotizacionesDao,
            IConfigComisionesBanco configComisionesBancoDao, ICargoPago cargoPagoDao,
            FileStorageService fileStorageService) {
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
        this.fileStorageService = fileStorageService;
    }

    // ==================== SOLICITUD COMPLETA ====================

    @PostMapping("/guardar-solicitud-completa")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardarSolicitudCompleta(@RequestBody SolicitudPago payload) {
        if (payload.getProveedores() != null) {
            for (SolicitudProveedor prov : payload.getProveedores()) {
                if (prov.getDetallesAEliminar() != null) {
                    for (Long idDetalleDel : prov.getDetallesAEliminar()) {
                        DetalleSolicitud detDel = new DetalleSolicitud();
                        detDel.setIdDetalle(idDetalleDel);
                        detDel.setAudUsuario(payload.getAudUsuario());
                        ejecutar(detalleSolicitudDao.registrarDetalleSolicitud(detDel, "D"), "Error eliminando factura ID=" + idDetalleDel);
                    }
                }
            }
        }
        if (payload.getProveedoresAEliminar() != null) {
            for (Long idProvDel : payload.getProveedoresAEliminar()) {
                SolicitudProveedor provDel = new SolicitudProveedor();
                provDel.setIdSolicitudProveedor(idProvDel);
                provDel.setAudUsuario(payload.getAudUsuario());
                ejecutar(solicitudProveedorDao.registrarSolicitudProveedor(provDel, "D"), "Error eliminando proveedor ID=" + idProvDel);
            }
        }
        String accionSol = payload.getIdSolicitud() == 0 ? "I" : "U";
        RespuestaSp resSol = solicitudPagoDao.registrarSolicitudPago(payload, accionSol);
        ejecutar(resSol, "Error en cabecera de solicitud");
        long idSolicitud = resSol.getIdGenerado() > 0 ? resSol.getIdGenerado() : payload.getIdSolicitud();
        if (payload.getProveedores() != null) {
            for (SolicitudProveedor prov : payload.getProveedores()) {
                prov.setIdSolicitud(idSolicitud);
                if (prov.getAudUsuario() == 0) prov.setAudUsuario(payload.getAudUsuario());
                String accionProv = prov.getIdSolicitudProveedor() == 0 ? "I" : "U";
                RespuestaSp resProv = solicitudProveedorDao.registrarSolicitudProveedor(prov, accionProv);
                ejecutar(resProv, "Error en proveedor " + prov.getCardCode());
                long idProveedor = resProv.getIdGenerado() > 0 ? resProv.getIdGenerado() : prov.getIdSolicitudProveedor();
                if (prov.getDetalles() != null) {
                    for (DetalleSolicitud det : prov.getDetalles()) {
                        det.setIdSolicitudProveedor(idProveedor);
                        if (det.getAudUsuario() == 0) det.setAudUsuario(payload.getAudUsuario());
                        String accionDet = det.getIdDetalle() == 0 ? "I" : "U";
                        RespuestaSp resDet = detalleSolicitudDao.registrarDetalleSolicitud(det, accionDet);
                        ejecutar(resDet, "Error en factura " + det.getNumeroDocumento());
                    }
                }
            }
        }
        return respuestaCreada(idSolicitud);
    }

    @PostMapping("/aprobar-solicitud")
    @Transactional
    public ResponseEntity<ApiResponse<?>> aprobarSolicitud(@RequestBody SolicitudPago payload) {
        SolicitudPago actual = solicitudPagoDao.obtenerSolicitudPagoPorId(payload.getIdSolicitud());
        if (actual == null) throw new SpBusinessException("No se encontró la solicitud con ID: " + payload.getIdSolicitud());
        actual.setEstado(payload.getEstado());
        actual.setAudUsuario(payload.getAudUsuario());
        RespuestaSp res = solicitudPagoDao.registrarSolicitudPago(actual, "U");
        ejecutar(res, "Error cambiando estado de solicitud");
        return respuestaCreada(payload.getIdSolicitud());
    }

    // ==================== COTIZACIÓN ====================

    @PostMapping("/guardar-cotizacion-completa")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardarCotizacionCompleta(@RequestBody Cotizaciones payload) {
        boolean esNueva = payload.getIdCotizacion() == 0;
        String accion = esNueva ? "I" : "U";
        RespuestaSp res = cotizacionesDao.registrarCotizaciones(payload, accion);
        ejecutar(res, "Error registrando cotización");
        long idCotizacion = res.getIdGenerado() > 0 ? res.getIdGenerado() : payload.getIdCotizacion();
        if (esNueva && payload.getCargos() != null) {
            int ordenAuto = 1;
            for (CargoPago cargo : payload.getCargos()) {
                cargo.setIdCotizacion(idCotizacion);
                cargo.setIdTransaccion(0L);
                cargo.setAudUsuario(payload.getAudUsuario());
                if (cargo.getOrden() == 0) cargo.setOrden(ordenAuto);
                ordenAuto++;
                RespuestaSp resCargo = cargoPagoDao.registrarCargoPago(cargo, "I");
                ejecutar(resCargo, "Error registrando cargo en cotización");
            }
        }
        return respuestaCreada(idCotizacion);
    }

    @PostMapping("/aceptar-cotizacion")
    @Transactional
    public ResponseEntity<ApiResponse<?>> aceptarCotizacion(@RequestBody Cotizaciones payload) {
        RespuestaSp res = cotizacionesDao.registrarCotizaciones(payload, "U");
        ejecutar(res, "Error aceptando cotización");
        return respuestaCreada(payload.getIdCotizacion());
    }

    // ==================== TRANSACCIÓN ====================

    @PostMapping("/guardar-transaccion-completa")
    @Transactional
    public ResponseEntity<ApiResponse<?>> guardarTransaccionCompleta(@RequestBody Transacciones payload) {
        String accion = payload.getIdTransaccion() == 0 ? "I" : "U";
        RespuestaSp res = transaccionesDao.registrarTransacciones(payload, accion);
        ejecutar(res, "Error registrando transacción");
        long idTransaccion = res.getIdGenerado() > 0 ? res.getIdGenerado() : payload.getIdTransaccion();
        if (payload.getIdTransaccion() == 0 && payload.getCargos() != null) {
            int ordenAuto = 1;
            for (CargoPago cargo : payload.getCargos()) {
                cargo.setIdTransaccion(idTransaccion);
                cargo.setIdCotizacion(0L);
                cargo.setAudUsuario(payload.getAudUsuario());
                if (cargo.getOrden() == 0) cargo.setOrden(ordenAuto);
                ordenAuto++;
                RespuestaSp resCargo = cargoPagoDao.registrarCargoPago(cargo, "I");
                ejecutar(resCargo, "Error registrando cargo en transacción");
            }
        }
        return respuestaCreada(idTransaccion);
    }

    @PostMapping("/cambiar-estado-transaccion")
    @Transactional
    public ResponseEntity<ApiResponse<?>> cambiarEstadoTransaccion(@RequestBody Transacciones payload) {
        Transacciones actual = transaccionesDao.obtenerTransaccionPorId(payload.getIdTransaccion(), payload.getCodEmpresa());
        if (actual == null) throw new SpBusinessException("No se encontró la transacción con ID: " + payload.getIdTransaccion());
        actual.setEstado(payload.getEstado());
        actual.setAudUsuario(payload.getAudUsuario());
        if (payload.getNumeroTransaccion() != null) actual.setNumeroTransaccion(payload.getNumeroTransaccion());
        if (payload.getFechaValor() != null) actual.setFechaValor(payload.getFechaValor());
        RespuestaSp res = transaccionesDao.registrarTransacciones(actual, "U");
        ejecutar(res, "Error cambiando estado de transacción");
        return respuestaCreada(payload.getIdTransaccion());
    }

    @PostMapping("/confirmar-pago")
    @Transactional
    public ResponseEntity<ApiResponse<?>> confirmarPago(@RequestBody ConfirmarPagoRequest payload) {
        Transacciones trxActual = transaccionesDao.obtenerTransaccionPorId(payload.getIdTransaccion(), payload.getCodEmpresa());
        if (trxActual == null) throw new SpBusinessException("No se encontró la transacción con ID: " + payload.getIdTransaccion());
        trxActual.setEstado("CONFIRMADO");
        trxActual.setAudUsuario(payload.getAudUsuario());
        if (payload.getNumeroTransaccion() != null) trxActual.setNumeroTransaccion(payload.getNumeroTransaccion());
        if (payload.getFechaValor() != null) trxActual.setFechaValor(payload.getFechaValor());
        RespuestaSp resTrx = transaccionesDao.registrarTransacciones(trxActual, "U");
        ejecutar(resTrx, "Error confirmando transacción");
        SolicitudPago solActual = solicitudPagoDao.obtenerSolicitudPagoPorId(payload.getIdSolicitud());
        if (solActual == null) throw new SpBusinessException("No se encontró la solicitud con ID: " + payload.getIdSolicitud());
        solActual.setEstado("PAGADA");
        solActual.setAudUsuario((int) payload.getAudUsuario());
        if (payload.getMontoTotalSolicitud() > 0) solActual.setMontoTotalSolicitud(payload.getMontoTotalSolicitud());
        RespuestaSp resSol = solicitudPagoDao.registrarSolicitudPago(solActual, "U");
        ejecutar(resSol, "Error cerrando solicitud como PAGADA");
        return respuestaCreada(payload.getIdSolicitud());
    }

    // ==================== ENDPOINTS DE CONSULTA ====================

    @PostMapping("/obtener-solicitudes")
    public ResponseEntity<ApiResponse<?>> obtenerSolicitudes(@RequestBody SolicitudPago mb) {
        return procesarLista(solicitudPagoDao.obtenerSolicitudPago(mb.getIdSolicitud()), "No se encontraron solicitudes.");
    }

    @PostMapping("/obtener-solicitud-proveedor")
    public ResponseEntity<ApiResponse<?>> obtenerSolicitudProveedor(@RequestBody SolicitudProveedor mb) {
        return procesarLista(solicitudProveedorDao.obtenerSolicitudProveedor(mb.getIdSolicitudProveedor()), "No se encontró el proveedor.");
    }

    @PostMapping("/obtener-detalle-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerDetalleSolicitud(@RequestBody DetalleSolicitud mb) {
        return procesarLista(detalleSolicitudDao.obtenerDetalleSolicitud(mb.getIdDetalle()), "No se encontró el detalle.");
    }

    @PostMapping("/obtener-proveedores-empresa")
    public ResponseEntity<ApiResponse<?>> obtenerProveedoresXEmpresa(@RequestBody SolicitudProveedor mb) {
        return procesarLista(solicitudProveedorDao.obtenerProveedoresXEmpresa(mb.getCodEmpresa()), "No se encontraron proveedores.");
    }

    @PostMapping("/obtener-docnum-empresa")
    public ResponseEntity<ApiResponse<?>> obtenerDocumentosPorEmpresa(@RequestBody DetalleSolicitud mb) {
        return procesarLista(detalleSolicitudDao.obtenerFacProvYOrdCompraXEmpresa(mb.getCodEmpresa()), "No se encontraron documentos.");
    }

    @PostMapping("/reporte-solicitudes-fechas")
    public ResponseEntity<ApiResponse<?>> reporteSolicitudesXFecha(@RequestBody FiltroFechasDto filtro) {
        return procesarLista(solicitudPagoDao.reporteSolicitudesXFecha(filtro.getFechaInicio(), filtro.getFechaFin()), "No se encontraron solicitudes.");
    }

    @PostMapping("/obtener-cotizaciones-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerCotizacionesPorSolicitud(@RequestBody Cotizaciones mb) {
        return procesarLista(cotizacionesDao.obtenerCotizacionesPorSolicitud(mb.getIdSolicitud()), "No se encontraron cotizaciones.");
    }

    @PostMapping("/obtener-transacciones-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccionesPorSolicitud(@RequestBody Transacciones mb) {
        return procesarLista(transaccionesDao.obtenerTransacciones(mb.getIdSolicitud(), mb.getCardCode(), mb.getCodBanco(), mb.getEstado(), mb.getIdTipoTransaccion(), mb.getCodEmpresa()), "No se encontraron transacciones.");
    }

    @PostMapping("/obtener-transacciones-cotizacion")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccionesPorCotizacion(@RequestBody Transacciones mb) {
        return procesarLista(transaccionesDao.obtenerTransaccionesPorCotizacion(mb.getIdCotizacion()), "No se encontraron transacciones.");
    }

    @PostMapping("/obtener-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerTransaccion(@RequestBody Transacciones mb) {
        Transacciones result = transaccionesDao.obtenerTransaccion(mb.getIdTransaccion(), mb.getCodEmpresa());
        if (result == null) return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ApiResponse<>("No se encontró la transacción.", null, HttpStatus.NO_CONTENT.value()));
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, result, HttpStatus.OK.value()));
    }

    @PostMapping("/reporte-transacciones-fechas")
    public ResponseEntity<ApiResponse<?>> reporteTransaccionesXFecha(@RequestBody Transacciones mb) {
        return procesarLista(transaccionesDao.obtenerTransaccionesReporte(mb.getFechaInicio(), mb.getFechaFin(), mb.getCardCode(), mb.getEstado(), mb.getIdTipoTransaccion(), mb.getCodEmpresa()), "No se encontraron transacciones.");
    }

    @PostMapping("/obtener-cargos-cotizacion")
    public ResponseEntity<ApiResponse<?>> obtenerCargosPorCotizacion(@RequestBody CargoPago mb) {
        return procesarLista(cargoPagoDao.obtenerCargoPorCotizacion(mb.getIdCotizacion()), "No se encontraron cargos.");
    }

    @PostMapping("/obtener-cargos-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerCargosPorTransaccion(@RequestBody CargoPago mb) {
        return procesarLista(cargoPagoDao.obtenerCargoPorTransaccion(mb.getIdTransaccion()), "No se encontraron cargos.");
    }

    @PostMapping("/obtener-log-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerLogPorSolicitud(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerLogPorSolicitud(mb.getIdSolicitud()), "No se encontraron logs.");
    }

    @PostMapping("/obtener-log-transaccion")
    public ResponseEntity<ApiResponse<?>> obtenerLogPorTransaccion(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerLogPorTransaccion(mb.getIdTransaccion()), "No se encontraron logs.");
    }

    @PostMapping("/obtener-timeline-solicitud")
    public ResponseEntity<ApiResponse<?>> obtenerTimelineSolicitud(@RequestBody LogEstados mb) {
        return procesarLista(logEstadosDao.obtenerTimelineCompleto(mb.getIdSolicitud()), "No se encontraron eventos.");
    }

    // ==================== CATÁLOGOS ====================

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
        return procesarLista(canalesPagoDao.obtenerCanalesPago(mb.getIdCanal()), "No se encontraron canales.");
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
        return procesarLista(tiposCambioDao.obtenerTiposCambioPorBanco(mb.getCodBanco()), "No se encontraron tipos de cambio.");
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
        return procesarLista(tiposTransaccionDao.obtenerTiposTransaccion(mb.getIdTipoTransaccion()), "No se encontraron tipos de transacción.");
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
        return procesarLista(configComisionesBancoDao.obtenerConfigComisionesBanco(mb.getIdConfig()), "No se encontró la configuración.");
    }

    @PostMapping("/obtener-config-comisiones-banco")
    public ResponseEntity<ApiResponse<?>> obtenerConfigComisionesPorBanco(@RequestBody ConfigComisionesBanco mb) {
        return procesarLista(configComisionesBancoDao.obtenerConfigPorBanco(mb.getCodBanco()), "No se encontraron configuraciones.");
    }

    // ==================== VOUCHERS ====================

    @PostMapping(value = "/transacciones/{idTransaccion}/voucher", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> subirVoucher(@PathVariable long idTransaccion, @RequestParam("file") MultipartFile file, @RequestParam("audUsuario") int audUsuario) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("El archivo no puede estar vacío.", null, HttpStatus.BAD_REQUEST.value()));
        }
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "Nombre de archivo no disponible");
        String ext = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!EXTENSIONES_VOUCHER.contains(ext)) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("Extensión no permitida. Use PDF, JPG, JPEG o PNG.", null, HttpStatus.BAD_REQUEST.value()));
        }
        String rutaRelativa = "pagos-extranjeros/vouchers/" + idTransaccion + "_" + System.currentTimeMillis() + "." + ext;
        try {
            fileStorageService.guardarVoucher(file, rutaRelativa);
            try {
                transaccionesDao.actualizarVoucher(idTransaccion, rutaRelativa, audUsuario);
            } catch (SpBusinessException spEx) {
                fileStorageService.eliminarVoucher(rutaRelativa);
                log.warn("Voucher eliminado por error en SP. Transacción ID={}: {}", idTransaccion, spEx.getMessage());
                throw spEx;
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(SUCCESS_MESSAGE, rutaRelativa, HttpStatus.CREATED.value()));
        } catch (SpBusinessException ex) {
            throw ex;
        } catch (IOException e) {
            log.error("Error guardando voucher para transacción ID={}: {}", idTransaccion, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>("Error al guardar el archivo.", null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/transacciones/{idTransaccion}/voucher")
    public ResponseEntity<?> obtenerVoucher(@PathVariable long idTransaccion, @RequestParam(value = "codEmpresa", required = false, defaultValue = "0") long codEmpresa) {
        Transacciones trx = transaccionesDao.obtenerTransaccionPorId(idTransaccion, codEmpresa);
        if (trx == null || trx.getRutaVoucher() == null || Boolean.FALSE.equals(trx.getTieneVoucher())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>("La transacción no tiene voucher registrado.", null, HttpStatus.NOT_FOUND.value()));
        }
        try {
            Resource resource = fileStorageService.obtenerVoucher(trx.getRutaVoucher());
            String rutaVoucher = trx.getRutaVoucher();
            String ext = rutaVoucher.contains(".") ? rutaVoucher.substring(rutaVoucher.lastIndexOf('.') + 1).toLowerCase() : "";
            String contentType;
            switch (ext) {
                case "pdf": contentType = "application/pdf"; break;
                case "jpg":
                case "jpeg": contentType = "image/jpeg"; break;
                case "png": contentType = "image/png"; break;
                default: contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).contentLength(resource.contentLength())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"").body(resource);
        } catch (IOException e) {
            log.error("Error leyendo voucher para transacción ID={}: {}", idTransaccion, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>("Error al leer el archivo.", null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void ejecutar(RespuestaSp res, String contexto) {
        if (res.getError() != 0) throw new SpBusinessException(contexto + ": " + res.getErrormsg());
    }

    private ResponseEntity<ApiResponse<?>> respuestaCreada(long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(SUCCESS_MESSAGE, id, HttpStatus.CREATED.value()));
    }

    private ResponseEntity<ApiResponse<?>> respuestaEscritura(RespuestaSp res) {
        HttpStatus status = res.getError() == 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), status.value()));
    }

    private <T> ResponseEntity<ApiResponse<?>> procesarLista(List<T> lista, String mensajeVacio) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new ApiResponse<>(mensajeVacio, null, HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
}
