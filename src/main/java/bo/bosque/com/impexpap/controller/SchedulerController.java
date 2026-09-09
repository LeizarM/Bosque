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

    /**
     * Crea el rol de sábados de la gestión siguiente, si todavía no existe.
     * POST /scheduler/generar-rol-gestion-siguiente
     *
     * <p><b>Es la recuperación de la ventana perdida.</b> El cron corre del 22 al 31
     * de diciembre; si el servidor estuvo apagado esos diez días, desde aquí se
     * dispara la misma tarea el 2 de enero — sin mecanismo nuevo y sin tocar el
     * módulo de RR.HH.
     *
     * <p>Es idempotente y siempre responde 200: si el rol ya existe no hace nada, y
     * eso no es un error. La tarea se traga sus propias excepciones y las loguea, así
     * que el detalle de qué pasó está en el log del servidor.
     */
    @Secured("ROLE_ADM")
    @PostMapping("/generar-rol-gestion-siguiente")
    public ResponseEntity<ApiResponse<String>> generarRolGestionSiguiente() {
        scheduler.generarRolGestionSiguiente();
        return ResponseEntity.ok(
            new ApiResponse<>("Tarea del rol de la gestión siguiente ejecutada", null, 200));
    }

    /**
     * Genera las ocurrencias de tareas rutinarias del día, sin esperar a las 00:05.
     * POST /scheduler/generar-tareas-rutinarias
     *
     * <p>Los otros dos jobs de {@code DatabaseTaskScheduler} ya tenían su disparador
     * manual; este completa el juego. Sirve sobre todo para comprobar en el momento
     * que el generador dejó de producir los flujos marcados como "a requerimiento"
     * (Caja Fuerte, Coches, Caja Chica y Cierre de Operaciones) sin tener que esperar
     * a la madrugada.
     *
     * <p>Es idempotente: el SP hace {@code EXCEPT} contra lo ya generado, así que
     * dispararlo dos veces el mismo día no duplica nada.
     */
    @Secured("ROLE_ADM")
    @PostMapping("/generar-tareas-rutinarias")
    public ResponseEntity<ApiResponse<String>> generarTareasRutinarias() {
        scheduler.generarOcurrenciasTareasRutinarias();
        return ResponseEntity.ok(
            new ApiResponse<>("Generación de tareas rutinarias ejecutada", null, 200));
    }
}

