package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IPrestamo;
import bo.bosque.com.impexpap.model.Prestamo;
import bo.bosque.com.impexpap.model.PrestamoDetalle;
import bo.bosque.com.impexpap.utils.Tipos;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/prestamos")
@CrossOrigin(origins = "*", methods = RequestMethod.POST)
public class PrestamoController {

    private static final String SUCCESS_MESSAGE = "OPERACION REALIZADA EXITOSAMENTE";

    @Autowired
    private IPrestamo prestamoDao;

    @Autowired
    private JasperReportExport jasperReportExport;

    /**
     * listado de prestamos provenientes de SAP (cruza con BOSQUE para ver estado de
     * asignacion)
     * 
     * @param p
     * @return
     */
    @PostMapping("/listarPrestamosSAP")
    public ResponseEntity<ApiResponse<?>> listarPrestamosSAP(@RequestBody Prestamo p) {
        List<Prestamo> lista = prestamoDao.obtenerPrestamosSAP(p);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    @PostMapping("/estados")
    public ResponseEntity<ApiResponse<?>> listarEstadosPrestamo(@RequestBody Prestamo p) {
        List<Tipos> rs = prestamoDao.listEstadosPrestamo(p);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, rs, HttpStatus.OK.value()));
    }

    @PostMapping("/tiposPago")
    public ResponseEntity<ApiResponse<?>> listarTiposPagoPrestamo(@RequestBody Prestamo p) {
        List<Tipos> rs = prestamoDao.listTiposPagoPrestamo(p);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, rs, HttpStatus.OK.value()));
    }

    /**
     * Asignación Masiva de Préstamos
     * 
     * @param p
     * @param request
     * @return
     */
    @PostMapping("/asignarPrestamosMasivo")
    public ResponseEntity<ApiResponse<?>> asignarPrestamosMasivo(@RequestBody Prestamo p) {
        RespuestaSp rs = prestamoDao.registrarPrestamo(p, "XM");
        // Si no hay error (si hay error, SpHelper lanza SpBusinessException)
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(rs.getErrormsg(), Collections.emptyList(), HttpStatus.OK.value()));
    }

    /**
     * Edición Masiva de Préstamos (SAP o Manuales múltiples)
     * 
     * @param p
     * @return
     */
    @PostMapping("/editarPrestamoMasivo")
    public ResponseEntity<ApiResponse<?>> editarPrestamoMasivo(@RequestBody Prestamo p) {
        RespuestaSp rs = prestamoDao.registrarPrestamo(p, "UX");
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(rs.getErrormsg(), Collections.emptyList(), HttpStatus.OK.value()));
    }

    /**
     * listado de detalles de un préstamo
     * 
     * @param p
     * @return
     */
    @PostMapping("/listarDetalles")
    public ResponseEntity<ApiResponse<?>> listarDetallesPrestamo(@RequestBody Prestamo p) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, prestamoDao.obtenerDetallesPrestamo(p),
                        HttpStatus.OK.value()));
    }

    @PostMapping("/previsualizarCuotas")
    public ResponseEntity<ApiResponse<?>> previsualizarCuotas(@RequestBody Prestamo p) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, prestamoDao.previsualizarCuotas(p), HttpStatus.OK.value()));
    }

    /**
     * listado de empleados asignados a un prestamo
     * 
     * @param p
     * @return
     */
    @PostMapping("/listarEmpleadosAsignados")
    public ResponseEntity<ApiResponse<?>> listarEmpleadosAsignados(@RequestBody Prestamo p) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, prestamoDao.listarEmpleadosPrestamo(p),
                        HttpStatus.OK.value()));
    }

    /**
     * Actualiza un detalle de la cuota del préstamo
     * 
     * @param payload (codPrestDetalle, tipoPago, fechaPago, audUsuario, ACCION)
     * @return
     */
    @PostMapping("/actualizarDetalle")
    public ResponseEntity<ApiResponse<?>> actualizarDetallePrestamo(@RequestBody PrestamoDetalle p) {

        RespuestaSp rs = prestamoDao.actualizarCuotaPrestamo(p, "AC");

        if (rs.getError() != 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(rs.getErrormsg(), Collections.emptyList(), HttpStatus.BAD_REQUEST.value()));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(rs.getErrormsg(), Collections.emptyList(), HttpStatus.OK.value()));
    }

    /**
     * Adelanta un pago y recalcula cuotas
     * 
     * @param p (codPrestamo, montoPago, fechaPago, audUsuario)
     * @return
     */
    @PostMapping("/adelantarCuota")
    public ResponseEntity<ApiResponse<?>> adelantarCuota(@RequestBody PrestamoDetalle p) {

        RespuestaSp rs = prestamoDao.adelantarCuota(p, "AD");

        if (rs.getError() != 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(rs.getErrormsg(), Collections.emptyList(), HttpStatus.BAD_REQUEST.value()));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(rs.getErrormsg(), Collections.emptyList(), HttpStatus.OK.value()));
    }

    @PostMapping("/anularPrestamo")
    public ResponseEntity<ApiResponse<?>> anularPrestamo(@RequestBody Prestamo p) {
        RespuestaSp rs = prestamoDao.registrarPrestamo(p, "AN");
        if (rs.getError() != 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(rs.getErrormsg(), Collections.emptyList(), HttpStatus.BAD_REQUEST.value()));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(rs.getErrormsg(), Collections.emptyList(), HttpStatus.OK.value()));
    }

    @PostMapping("/reporteCuotas")
    public ResponseEntity<?> reporteCuotas(@RequestBody Prestamo p) {
        Map<String, Object> params = new HashMap<>();
        params.put("codPrestamo", p.getCodPrestamo());
        return generarReporte("RptCuotas", params);
    }

    private ResponseEntity<?> generarReporte(String reportName, Map<String, Object> params) {
        try {
            byte[] reportBytes = this.jasperReportExport.exportPDFStatic(reportName, params);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<?> generarReporte(String reportName, Prestamo p) {
        Map<String, Object> params = new HashMap<>();
        if (p.getCodEmpleado() != null) {
            params.put("codEmpleado", p.getCodEmpleado());
        }
        if (p.getFechaDesde() != null) {
            params.put("fechaDesde", p.getFechaDesde());
        }
        if (p.getFechaHasta() != null) {
            params.put("fechaHasta", p.getFechaHasta());
        }
        return generarReporte(reportName, params);
    }

    @PostMapping("/reportePersonal")
    public ResponseEntity<?> reportePersonal(@RequestBody Prestamo p) {
        return generarReporte("RptPrestamosPersonal", p);
    }

    @PostMapping("/reporteMayorGlobalResumido")
    public ResponseEntity<?> reporteMayorGlobalResumido(@RequestBody Prestamo p) {
        return generarReporte("RptPrestamoMayorGlobalResumido", p);
    }

    @PostMapping("/reporteGlobalDetallado")
    public ResponseEntity<?> reporteGlobalDetallado(@RequestBody Prestamo p) {
        return generarReporte("RptPrestamoMayorGlobalDetallado", p);
    }

    @PostMapping("/reporteCortoLargoPlazo")
    public ResponseEntity<?> reporteCortoLargoPlazo(@RequestBody Prestamo p) {
        return generarReporte("RptPrestamosCortoLargoPlazo", p);
    }

    @PostMapping("/reporteMayorGeneral")
    public ResponseEntity<?> reporteMayorGeneral(@RequestBody Prestamo p) {
        return generarReporte("RptPrestamosMayorGeneral", p);
    }
}
