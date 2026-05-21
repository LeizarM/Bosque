package bo.bosque.com.impexpap.scheduler;

import bo.bosque.com.impexpap.commons.WhatsAppService;
import bo.bosque.com.impexpap.dao.IEmpleado;
import bo.bosque.com.impexpap.dao.IEntregaChofer;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.EntregaChofer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

@Component

public class DatabaseTaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTaskScheduler.class);
    private final IEntregaChofer entregaChoferDao;
    private final IEmpleado empleadoDao;
    private final WhatsAppService whatsAppService;

    public DatabaseTaskScheduler(IEntregaChofer entregaChoferDao,
                                 IEmpleado empleadoDao,
                                 WhatsAppService whatsAppService) {
        this.entregaChoferDao = entregaChoferDao;
        this.empleadoDao = empleadoDao;
        this.whatsAppService = whatsAppService;
    }

    /**
     * Este método se ejecuta cada día a las 23:58:59
     */
    @Scheduled(cron = "59 58 23 * * *")
    public void executeDailyStoredProcedure() {
        try {
            logger.info("Iniciando ejecución del procedimiento almacenado ::: p_abm_trch_Entregas");
            this.entregaChoferDao.registrarEntregaChofer(new EntregaChofer(), "C");
            whatsAppService.enviarNotificacionAGrupos("Test BOT", "Procedimiento almacenado ejecutado exitosamente para cerrar entregas abiertas");
            logger.info("Procedimiento almacenado ejecutado exitosamente ::: p_abm_trch_Entregas");
        } catch (Exception e) {
            logger.error("Error al ejecutar el procedimiento p_abm_trch_Entregas: {}", e.getMessage(), e);
        }
    }

    /**
     * Notifica los cumpleaños del día a las 07:30 am.
     * Obtiene la lista completa de empleados con fecha de cumpleaños y filtra
     * los que cumplen años hoy (mismo día y mes).
     */
    @Scheduled(cron = "0 30 7 * * *")
    public void notificarCumpleaniosDelDia() {
        logger.info("Iniciando verificación de cumpleaños del día");
        try {
            List<Empleado> todos = empleadoDao.listaCumpleEmpleado();

            if (todos == null || todos.isEmpty()) {
                logger.info("No se encontraron empleados en la lista de cumpleaños");
                return;
            }

            LocalDate hoy = LocalDate.now();
            int diaHoy = hoy.getDayOfMonth();
            int mesHoy = hoy.getMonthValue();

            List<Empleado> cumpleaneros = todos.stream()
                    .filter(emp -> emp.getPersona() != null
                            && emp.getPersona().getFechaNacimiento() != null)
                    .filter(emp -> {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(emp.getPersona().getFechaNacimiento());
                        return cal.get(Calendar.DAY_OF_MONTH) == diaHoy
                                && (cal.get(Calendar.MONTH) + 1) == mesHoy;
                    })
                    .collect(Collectors.toList());

            if (cumpleaneros.isEmpty()) {
                logger.info("No hay empleados que cumplan años hoy ({})", hoy);
                whatsAppService.enviarNotificacionAGrupos("Cumpleaños",
                        "🎂 *Cumpleaños del día " + hoy + "*\n\nNo hay cumpleaños hoy.");
                return;
            }

            StringBuilder mensaje = new StringBuilder();
            mensaje.append("🎂 *Cumpleaños del día ").append(hoy).append("*\n\n");
            for (Empleado emp : cumpleaneros) {
                String nombre = emp.getPersona().getDatoPersona() != null
                        ? emp.getPersona().getDatoPersona() : "Sin nombre";
                String sucursal = (emp.getSucursal() != null && emp.getSucursal().getNombre() != null)
                        ? emp.getSucursal().getNombre() : "";
                mensaje.append("🎉 ").append(nombre);
                if (!sucursal.isEmpty()) {
                    mensaje.append(" — ").append(sucursal);
                }
                mensaje.append("\n");
            }

            whatsAppService.enviarNotificacionAGrupos("Cumpleaños", mensaje.toString());
            logger.info("Notificación de cumpleaños enviada para {} empleado(s)", cumpleaneros.size());

        } catch (Exception e) {
            logger.error("Error al notificar cumpleaños del día: {}", e.getMessage(), e);
        }
    }
}