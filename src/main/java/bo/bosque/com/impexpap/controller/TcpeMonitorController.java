package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.dao.ITcpeMonitor;
import bo.bosque.com.impexpap.dto.TcpeMonitorFiltroDto;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panel "En vivo" del evaluador (contrato 8): lotes para monitorear, estado de cada evaluado y espejo de la pregunta
 * que tiene delante, con {@code p_list_tcpe_Monitor}. Exige la pantalla {@value TcpePermisos#EN_VIVO} o
 * {@value TcpePermisos#RESULTADOS}; el administrador pasa siempre.
 */
@RestController
@CrossOrigin(originPatterns = "${tcpe.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/examenes/monitor")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TcpeMonitorController {

    private final ITcpeMonitor monitor;
    private final TcpePermisos permisos;

    public TcpeMonitorController(ITcpeMonitor monitor, TcpePermisos permisos) {
        this.monitor = monitor;
        this.permisos = permisos;
    }

    @PostMapping("/lotes")
    public ResponseEntity<ApiResponse<?>> lotes(Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.EN_VIVO, TcpePermisos.RESULTADOS);
        return TcpeRespuestas.listado(monitor.lotes());
    }

    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@RequestBody TcpeMonitorFiltroDto f, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.EN_VIVO, TcpePermisos.RESULTADOS);
        return TcpeRespuestas.listado(monitor.listar(f.getCodRolExamen()));
    }

    @PostMapping("/espejo")
    public ResponseEntity<ApiResponse<?>> espejo(@RequestBody TcpeMonitorFiltroDto f, Authentication auth) {
        permisos.exigirAlguna(auth, TcpePermisos.EN_VIVO, TcpePermisos.RESULTADOS);
        return TcpeRespuestas.listado(monitor.espejo(f.getCodExamen()));
    }
}
