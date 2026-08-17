package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IEmpleado;
import bo.bosque.com.impexpap.dao.IPermiso;
import bo.bosque.com.impexpap.dao.ISolicitudPermiso;
import bo.bosque.com.impexpap.model.DiaNoLaborable;
import bo.bosque.com.impexpap.model.Empleado;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin("*")
@RequestMapping("/vacacion")
public class VacacionController {
    private JdbcTemplate jdbcTemplate;

    private final IPermiso pDao;
    private final ISolicitudPermiso solicitudDao;
    private final FeriadoDao feriadoDao;
    /** Para el gate de la boleta: el ACL de botones y el codEmpleado del token. */
    private final AccesoModuloHelper acceso;
    /** Para el gate de la boleta: a quiénes puede ver el que pide (organigrama). */
    private final IEmpleado empDao;

    public VacacionController(JdbcTemplate jdbcTemplate, IPermiso pDao, ISolicitudPermiso solicitudDao,
            FeriadoDao feriadoDao, AccesoModuloHelper acceso, IEmpleado empDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.pDao = pDao;
        this.solicitudDao = solicitudDao;
        this.feriadoDao = feriadoDao;
        this.acceso = acceso;
        this.empDao = empDao;
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
    // public VacacionController(JdbcTemplate jdbcTemplate, IPermiso pDao,
    // ISolicitudPermiso solicitudDao) { ... }

    /**
     * REGISTRAR NUEVA SOLICITUD DE VACACION/PERMISO (Empleado)
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" }) // Ajusta los roles según tu app
    @PostMapping("/solicitar")
    public ResponseEntity<ApiResponse<?>> registrarSolicitud(@RequestBody SolicitudPermiso s) {
        // RespuestaSp res = solicitudDao.registrarSolicitud(s, "I");
        RespuestaSp res = solicitudDao.registrarSolicitud(s, s.getCodSolicitud() == 0 ? "I" : "U");
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
     * ANULAR SOLICITUD APROBADA
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/anular")
    public ResponseEntity<ApiResponse<?>> anularSolicitud(@RequestBody SolicitudPermiso s) {
        RespuestaSp res = solicitudDao.anularSolicitud(s, "AN");
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.OK.value()));
    }

    /**
     * LISTAR SOLICITUDES PERMISO - VACACION
     *
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
     *
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/solicitudesIndividuales")
    public ResponseEntity<ApiResponse<?>> listarMisSolicitudes(@RequestBody SolicitudPermiso filtro) {
        return procesarListaCambios(solicitudDao.listarMisSolicitudes(filtro));
    }

    /**
     * OBTENER PERMISOS Y VACACIONES PROXIMOS PARA EL DASHBOARD
     *
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/proximosPermisos")
    public ResponseEntity<ApiResponse<?>> proximosDashboard(@RequestBody SolicitudPermiso filtro) {
        return procesarListaCambios(solicitudDao.obtenerPermisosProximosDashboard(filtro));
    }

    /**
     * Devolera una lista de los tipos del permiso
     *
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tipoPermiso")
    public List<Tipos> listTipoPermiso(@RequestBody SolicitudPermiso s) {
        return this.solicitudDao.listTipoPermiso(s);
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
     * REPORTE PERMISO - VACACION (la boleta de un permiso concreto)
     *
     * <p><b>Este endpoint estaba abierto.</b> No tenía {@code @Secured}, así que caía en el
     * {@code anyRequest().authenticated()} de {@code MainSecurity}: cualquiera de los 134
     * usuarios, con cualquier token válido, podía enumerar {@code codPermiso} de 1 a 8459 y
     * bajarse la boleta de cualquier empleado —motivo del permiso incluido—. Es la deuda D4 del
     * plan de migración.
     *
     * <p><b>Por qué el gate no es sólo el botón del ACL.</b> Con {@code nivelAcceso=1},
     * {@code btnReImprimirBoleta} lo tienen <b>6 usuarios</b> más los 6 {@code ROLE_ADM}
     * <i>(verificado en la base)</i>. Hoy quien usa este endpoint es un JEFE mirando la ficha de
     * un dependiente: {@code MisSolicitudesWidget} → {@code rptPermisoVacacionProvider}, montado
     * dentro de {@code InfoEmpleadoScreen}, a la que se llega desde
     * {@code empleado_dependiente_screen.dart:1087}. Cerrar sólo con el ACL le sacaría la
     * función a todos los jefes que no son de RR.HH. Por eso la regla es un O:
     *
     * <ol>
     *   <li>tiene {@code btnReImprimirBoleta} (o es {@code ROLE_ADM}) → RR.HH. y Sistemas ven todo;</li>
     *   <li>la boleta es <b>suya</b>;</li>
     *   <li>la boleta es de alguien que <b>ya puede ver</b> en la ficha del trabajador.</li>
     * </ol>
     *
     * <p>El punto 3 no inventa una jerarquía nueva: pregunta por la MISMA lista que pinta la
     * pantalla desde la que se aprieta el botón ({@code p_list_Empleado @ACCION='X'}, el subárbol
     * de cargos del que pide). Si puede abrir la ficha, puede imprimir la boleta; si no, no. Así
     * el cambio cierra el agujero sin sacarle nada a nadie que hoy lo use legítimamente.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/RptPermisoVacacion")
    public ResponseEntity<?> RptPermisoVacacion(@RequestBody Permiso p, Authentication auth) {

        exigirAccesoABoleta(auth, p.getCodPermiso());

        String nombreReporte = "RptPermisoVacacion";

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("numPermiso", p.getCodPermiso());
            byte[] reportBytes = new JasperReportExport(this.jdbcTemplate).exportPDFStatic(nombreReporte, params);

            HttpHeaders headers = new HttpHeaders();
            // set the PDF format
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace(); // 🔥 Imprime el stack trace COMPLETO en consola para ver el error real
            System.err.println("Error detallado: " + e.getClass().getName() + " - " + e.getMessage()); // Imprime tipo y
            // mensaje
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * SUPUESTO D4 — pendiente de confirmación de RR.HH. (ver plan §5).
     *
     * <p>Las tres puertas de la boleta, en orden de costo: el ACL primero (una consulta que
     * además resuelve a todos los de RR.HH. y Sistemas), después la identidad, y sólo al final el
     * organigrama, que es la consulta cara.
     *
     * <p><b>La identidad sale del token, nunca del body.</b> El {@code codEmpleado} que manda el
     * cliente no se mira: si el cliente pudiera afirmar quién es, el gate no valdría nada.
     */
    private void exigirAccesoABoleta(Authentication auth, long codPermiso) {
        if (acceso.tieneBoton(auth, 24, "btnReImprimirBoleta")) return;

        Long dueno = pDao.codEmpleadoDeBoleta(codPermiso);
        if (dueno == null) {
            throw new SpBusinessException("No existe la boleta N° " + codPermiso + ".");
        }

        Long yo = acceso.codEmpleadoDelToken(auth);
        if (yo == null) {
            // Login sin empleado asociado en tb_usuario: no tiene boleta propia ni equipo.
            throw new AccessDeniedException("Tu usuario no esta asociado a ningun empleado.");
        }
        if (dueno.equals(yo)) return;                       // su propia boleta

        // El subárbol de cargos del que pide: exactamente la gente cuya ficha ya puede abrir.
        for (Empleado e : empDao.obtenerListaEmpleadoyDependientes(yo.intValue())) {
            if (dueno.intValue() == e.getCodEmpleado()) return;
        }
        throw new AccessDeniedException("Esa boleta no es tuya ni de tu equipo.");
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
