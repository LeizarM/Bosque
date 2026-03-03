package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IDetalleSolicitud;
import bo.bosque.com.impexpap.dao.ISolicitudPago;
import bo.bosque.com.impexpap.dao.ISolicitudProveedor;
import bo.bosque.com.impexpap.model.DetalleSolicitud;
import bo.bosque.com.impexpap.model.SolicitudProveedor;
import bo.bosque.com.impexpap.model.SolicitudPago;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pagos-extranjeros")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class PagosExtranjerosController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";

    private final ISolicitudPago solicitudPagoDao;
    private final ISolicitudProveedor solicitudProveedorDao;
    private final IDetalleSolicitud detalleSolicitudDao;

    public PagosExtranjerosController(ISolicitudPago solicitudPagoDao, ISolicitudProveedor solicitudProveedorDao, IDetalleSolicitud detalleSolicitudDao) {
        this.solicitudPagoDao = solicitudPagoDao;
        this.solicitudProveedorDao = solicitudProveedorDao;
        this.detalleSolicitudDao = detalleSolicitudDao;
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