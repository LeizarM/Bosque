package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IPlanilla;
import bo.bosque.com.impexpap.model.Planilla;
import bo.bosque.com.impexpap.model.PlanillaDetalle;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/planilla")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST})
@PreAuthorize("hasAnyRole('ROLE_ADM','ROLE_LIM')")
public class PlanillaController {

    private static final String SUCCESS_MESSAGE = "OPERACION REALIZADA EXITOSAMENTE";

    private final IPlanilla planillaDao;
    private final JdbcTemplate jdbcTemplate;

    public PlanillaController(IPlanilla planillaDao, JdbcTemplate jdbcTemplate) {
        this.planillaDao = planillaDao;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/listarPlanilla")
    public ResponseEntity<ApiResponse<?>> listarPlanilla(@RequestBody Planilla p) {
        List<Planilla> lista = planillaDao.listarPlanilla(p);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    @PostMapping("/listarPlanillaDetalle")
    public ResponseEntity<ApiResponse<?>> listarPlanillaDetalle(@RequestBody PlanillaDetalle pd) {
        List<PlanillaDetalle> lista = planillaDao.listarPlanillaDetalle(pd);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    @PostMapping("/generarPlanilla")
    public ResponseEntity<ApiResponse<?>> generarPlanilla(@RequestBody Planilla p) {
        RespuestaSp res = planillaDao.abmPlanilla(p, "G");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value()));
    }

    @PostMapping("/ejecutarPlanilla")
    public ResponseEntity<ApiResponse<?>> ejecutarPlanilla(@RequestBody Planilla p) {
        RespuestaSp res = planillaDao.abmPlanilla(p, "E");
        // El manejo del error lo gestiona SpHelper internamente, devolvemos simple.
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }

    @PostMapping("/pagosBancarios")
    public ResponseEntity<ApiResponse<?>> pagosBancarios(@RequestBody Map<String, Object> payload) {
        int mes = Integer.parseInt(payload.get("mes").toString());
        int anio = Integer.parseInt(payload.get("anio").toString());
        int codBanco = Integer.parseInt(payload.get("codBanco").toString());
        Integer codEmpresa = payload.containsKey("codEmpresa") && payload.get("codEmpresa") != null ? 
                             Integer.parseInt(payload.get("codEmpresa").toString()) : null;

        List<Map<String, Object>> lista = planillaDao.listarPagosBanco(mes, anio, codBanco, codEmpresa);
        
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    @PostMapping("/pdfEstimadoPagoBanco")
    public ResponseEntity<?> pdfEstimadoPagoBanco() {
        try {
            Map<String, Object> params = new HashMap<>();
            byte[] reportBytes = new JasperReportExport(this.jdbcTemplate).exportPDFStatic("RptPagoEstimadoBanco", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/pdfPlanillaCompacta")
    public ResponseEntity<?> pdfPlanillaCompacta(@RequestBody Planilla planilla) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("codPlanilla", planilla.getCodPlanilla() != null ? planilla.getCodPlanilla().intValue() : null);
            byte[] reportBytes = new JasperReportExport(this.jdbcTemplate).exportPDFStatic("RptPlanillaCompacto", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/excelPlanillaCompacta")
    public ResponseEntity<?> excelPlanillaCompacta(@RequestBody Planilla planilla) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("codPlanilla", planilla.getCodPlanilla() != null ? planilla.getCodPlanilla().intValue() : null);
            byte[] reportBytes = new JasperReportExport(this.jdbcTemplate).exportExcelStatic("RptExcPlanillaCompacto", params);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "PlanillaCompacta.xlsx");
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/pdfPlanillaExtendida")
    public ResponseEntity<?> pdfPlanillaExtendida(@RequestBody Planilla planilla) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("codPlanilla", planilla.getCodPlanilla() != null ? planilla.getCodPlanilla().intValue() : null);
            byte[] reportBytes = new JasperReportExport(this.jdbcTemplate).exportPDFStatic("RptPlanillaExtendida", params);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);
            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/pdfPapeletaPago")
    public ResponseEntity<?> pdfPapeletaPago(@RequestBody Planilla planilla) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("codPlanilla", planilla.getCodPlanilla() != null ? planilla.getCodPlanilla().intValue() : null);
            byte[] reportBytes = new JasperReportExport(this.jdbcTemplate).exportPDFStatic("RptPapeletaPago", params);

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
