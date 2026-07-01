package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.IPermiso;
import bo.bosque.com.impexpap.dao.ISolicitudPermiso;
import bo.bosque.com.impexpap.model.ChipTigo;
import bo.bosque.com.impexpap.model.DiaNoLaborable;
import bo.bosque.com.impexpap.model.Permiso;
import bo.bosque.com.impexpap.model.SolicitudPermiso;
import bo.bosque.com.impexpap.dao.FeriadoDao;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin ("*")
@RequestMapping ("/vacacion")
public class VacacionController {
    private JdbcTemplate jdbcTemplate;

    private final IPermiso pDao;
    private final ISolicitudPermiso solicitudDao;
    private final FeriadoDao feriadoDao;

    public VacacionController(JdbcTemplate jdbcTemplate
        ,IPermiso pDao, ISolicitudPermiso solicitudDao, FeriadoDao feriadoDao){
        this.jdbcTemplate = jdbcTemplate;
        this.pDao = pDao;
        this.solicitudDao = solicitudDao;
        this.feriadoDao = feriadoDao;
    }
    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";
    /**
     * OBTENER TOTAL DIAS DISPONIBLES DE VACACION EMPLEADO
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/diasDisponibles")
    public ResponseEntity<ApiResponse<?>> diasDisponibles(@RequestBody Permiso p) {
        return procesarListaCambios(pDao.diasDisponibles(p));
    }
    /**
     * Metodo auxiliar para procesar listas de cambios.
     * Devuelve 204 si no hay registros, 200 si hay.
     */
    private <T> ResponseEntity<ApiResponse<?>> procesarListaCambios(List<T> lista) {
        if (lista == null || lista.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new ApiResponse<>("No se encontraron registros.", null,
                            HttpStatus.NO_CONTENT.value()));
        }
        return ResponseEntity.ok(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
    // Arriba junto a los otros Daos, inyectamos el nuevo:
// private final ISolicitudPermiso solicitudDao;
// public VacacionController(JdbcTemplate jdbcTemplate, IPermiso pDao, ISolicitudPermiso solicitudDao) { ... }

    /**
     * REGISTRAR NUEVA SOLICITUD DE VACACION/PERMISO (Empleado)
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" }) // Ajusta los roles según tu app
    @PostMapping("/solicitar")
    public ResponseEntity<ApiResponse<?>> registrarSolicitud(@RequestBody SolicitudPermiso s) {
        //RespuestaSp res = solicitudDao.registrarSolicitud(s, "I");
        RespuestaSp res = solicitudDao.registrarSolicitud(s,s.getCodSolicitud()==0?"I":"U");
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }

    /**
     * APROBAR SOLICITUD PENDIENTE (Jefe Inmediato o RRHH)
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/aprobar")
    public ResponseEntity<ApiResponse<?>> aprobarSolicitud(@RequestBody SolicitudPermiso s) {
        RespuestaSp res = solicitudDao.aprobarSolicitud(s, "A");
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }

    /**
     * RECHAZAR SOLICITUD PENDIENTE
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/rechazar")
    public ResponseEntity<ApiResponse<?>> rechazarSolicitud(@RequestBody SolicitudPermiso s) {
        RespuestaSp res = solicitudDao.rechazarSolicitud(s, "R");
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }
    /**
     * LISTAR SOLICITUDES PERMISO - VACACION
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/pendientes")
    public ResponseEntity<ApiResponse<?>> listarPerdidas(@RequestBody SolicitudPermiso filtro) {
        return procesarListaCambios(solicitudDao.listarPendientes(filtro));
    }
    /**
     * LISTAR SOLICITUDES PROPIAS
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/solicitudesIndividuales")
    public ResponseEntity<ApiResponse<?>> listarMisSolicitudes(@RequestBody SolicitudPermiso filtro) {
        return procesarListaCambios(solicitudDao.listarMisSolicitudes(filtro));
    }
    /**
     * Devolera una lista de los tipos del permiso
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tipoPermiso")
    public List<Tipos> listTipoPermiso(){
        return this.solicitudDao.listTipoPermiso();
    }

    /**
     * OBTENER FERIADOS POR EMPLEADO
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/feriados")
    public ResponseEntity<ApiResponse<?>> listarFeriados(@RequestBody DiaNoLaborable filtro) {
        return procesarListaCambios(feriadoDao.obtenerFeriadosPorEmpleado(filtro));
    }
    /**
     * REPORTE PERMISO - VACACION
     * @param P
     * @return
     */
    @PostMapping("/RptPermisoVacacion")
    public ResponseEntity<?> RptPermisoVacacion(@RequestBody Permiso p)  {

        String nombreReporte = "RptPermisoVacacion";


        try{
            Map<String, Object> params = new HashMap<>();
            params.put("numPermiso", p.getCodPermiso() );
            byte[] reportBytes = new JasperReportExport( this.jdbcTemplate).exportPDFStatic( nombreReporte, params);


            HttpHeaders headers = new HttpHeaders();
            //set the PDF format
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes,headers ,HttpStatus.OK);
        } catch(Exception e) {
            e.printStackTrace();  // 🔥 Imprime el stack trace COMPLETO en consola para ver el error real
            System.err.println("Error detallado: " + e.getClass().getName() + " - " + e.getMessage());  // Imprime tipo y mensaje
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }
    /**
     * PREVISUALIZAR SALDO Y DIAS SOLICITADOS (Acción 'C')
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/previsualizarSaldo")
    public ResponseEntity<ApiResponse<?>> previsualizarSaldo(@RequestBody SolicitudPermiso filtro) {
        return procesarListaCambios(solicitudDao.previsualizarSaldo(filtro));
    }
}
