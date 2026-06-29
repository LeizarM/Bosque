package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.scheduler.DatabaseTaskScheduler;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints para disparar manualmente las tareas programadas.
 * Solo accesible por administradores.
 */
@RestController
@RequestMapping("/scheduler")
public class SchedulerController {

    private final DatabaseTaskScheduler scheduler;

    public SchedulerController(DatabaseTaskScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Dispara manualmente la notificación de cumpleaños del día.
     * POST /scheduler/notificar-cumpleanios
     */
    @Secured("ROLE_ADM")
    @PostMapping("/notificar-cumpleanios")
    public ResponseEntity<ApiResponse<String>> notificarCumpleanios() {
        scheduler.notificarCumpleaniosDelDia();
        return ResponseEntity.ok(new ApiResponse<>("Tarea de cumpleaños ejecutada", null, 200));
    }
}

