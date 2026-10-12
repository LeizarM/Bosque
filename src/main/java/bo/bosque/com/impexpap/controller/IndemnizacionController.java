package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.IIndemnizacion;
import bo.bosque.com.impexpap.model.Indemnizacion;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/indemnizacion")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
@PreAuthorize("hasAnyRole('ROLE_ADM','ROLE_LIM', 'ROLE_RRHH')")
public class IndemnizacionController {

    private static final String SUCCESS_MESSAGE = "OPERACION REALIZADA EXITOSAMENTE";

    private final IIndemnizacion indemnizacionDao;
    private final JasperReportExport jasperReportExport;

    public IndemnizacionController(IIndemnizacion indemnizacionDao, JasperReportExport jasperReportExport) {
        this.indemnizacionDao = indemnizacionDao;
        this.jasperReportExport = jasperReportExport;
    }

    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listarIndemnizacion(@RequestBody Indemnizacion indemnizacion) {
        List<Indemnizacion> lista = indemnizacionDao.listarIndemnizacion(indemnizacion);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    @PostMapping("/listar-historial")
    public ResponseEntity<ApiResponse<?>> listarHistorialIndemnizacion(@RequestBody Indemnizacion indemnizacion) {
        List<Indemnizacion> lista = indemnizacionDao.listarHistorialIndemnizacion(indemnizacion);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    @PostMapping("/abm")
    public ResponseEntity<ApiResponse<?>> abmIndemnizacion(@RequestBody Indemnizacion indemnizacion) {
        // En base al modelo, definimos la acción B (Insertar) o U (Actualizar) o D (Eliminar).
        // Nota: Asumiendo que B es insert en SpHelper.
        String accion = indemnizacion.getCodIndemnizacion() == null || indemnizacion.getCodIndemnizacion() == 0 ? "I" : "U";
        
        // Si el estado es 0, asumimos eliminacion
        if (indemnizacion.getEstado() != null && indemnizacion.getEstado() == 0) {
            accion = "D";
        }
        
        RespuestaSp res = indemnizacionDao.abmIndemnizacion(indemnizacion, accion);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value())
        );
    }

    @GetMapping("/reporte-provision")
    public ResponseEntity<ApiResponse<?>> reporteProvision(@RequestParam String fechaCorte) {
        List<java.util.Map<String, Object>> reporte = indemnizacionDao.reporteProvision(fechaCorte);
        return ResponseEntity.status(HttpStatus.OK).body(
                new ApiResponse<>(SUCCESS_MESSAGE, reporte, HttpStatus.OK.value())
        );
    }

    @PostMapping("/reporteIndemnizacion")
    public ResponseEntity<?> reporteIndemnizacion(@RequestBody(required = false) Map<String, Object> payload) {
        return generarReportePdf("RptIndemnizacion", "NORMAL", payload);
    }

    @PostMapping("/reporteIndemnizacionTodos")
    public ResponseEntity<?> reporteIndemnizacionTodos(@RequestBody(required = false) Map<String, Object> payload) {
        return generarReportePdf("RptIndemnizacion", "TODOS", payload);
    }

    @PostMapping("/reporteIndemnizacionRRHH")
    public ResponseEntity<?> reporteIndemnizacionRRHH(@RequestBody(required = false) Map<String, Object> payload) {
        return generarReportePdf("RptIndemnizacionRRHH", "RRHH", payload);
    }

    @PostMapping("/reporte-pdf")
    public ResponseEntity<?> reportePdfGeneral(@RequestBody(required = false) Map<String, Object> payload) {
        String tipo = "NORMAL";
        if (payload != null && payload.get("tipoReporte") != null) {
            tipo = payload.get("tipoReporte").toString().toUpperCase().trim();
        }
        String reportName = "RRHH".equals(tipo) ? "RptIndemnizacionRRHH" : "RptIndemnizacion";
        return generarReportePdf(reportName, tipo, payload);
    }

    private ResponseEntity<?> generarReportePdf(String reportName, String tipoReporte, Map<String, Object> payload) {
        try {
            Map<String, Object> params = new HashMap<>();

            // 1. fechaCorte
            Date fechaCorteDate = null;
            if (payload != null && payload.get("fechaCorte") != null) {
                String strFecha = payload.get("fechaCorte").toString().trim();
                if (!strFecha.isEmpty()) {
                    try {
                        fechaCorteDate = new SimpleDateFormat("yyyy-MM-dd").parse(strFecha);
                    } catch (Exception e1) {
                        try {
                            fechaCorteDate = new SimpleDateFormat("dd/MM/yyyy").parse(strFecha);
                        } catch (Exception e2) {
                            fechaCorteDate = new Date();
                        }
                    }
                }
            }
            if (fechaCorteDate == null) {
                fechaCorteDate = new Date();
            }
            params.put("fechaCorte", fechaCorteDate);



            // 3. tipoReporte
            params.put("tipoReporte", tipoReporte);

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
}
