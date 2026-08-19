package bo.bosque.com.impexpap.scheduler;

import bo.bosque.com.impexpap.commons.SincronizacionEntregasService;
import bo.bosque.com.impexpap.commons.WhatsAppService;
import bo.bosque.com.impexpap.dao.IEmpleado;
import bo.bosque.com.impexpap.dao.IEntregaChofer;
import bo.bosque.com.impexpap.dao.IProcesoRol;
import bo.bosque.com.impexpap.dao.IRol;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.EntregaChofer;
import bo.bosque.com.impexpap.model.Rol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component

public class DatabaseTaskScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTaskScheduler.class);
    private final IEntregaChofer entregaChoferDao;
    private final IEmpleado empleadoDao;
    private final WhatsAppService whatsAppService;
    private final IProcesoRol procesoRolDao;
    private final IRol rolDao;
    private final SincronizacionEntregasService sincronizacionEntregasService;

    public DatabaseTaskScheduler(IEntregaChofer entregaChoferDao,
                                 IEmpleado empleadoDao,
                                 WhatsAppService whatsAppService,
                                 IProcesoRol procesoRolDao,
                                 IRol rolDao,
                                 SincronizacionEntregasService sincronizacionEntregasService) {
        this.entregaChoferDao = entregaChoferDao;
        this.empleadoDao = empleadoDao;
        this.whatsAppService = whatsAppService;
        this.procesoRolDao = procesoRolDao;
        this.rolDao = rolDao;
        this.sincronizacionEntregasService = sincronizacionEntregasService;
    }

    /**
     * Mantiene {@code trch_Entregas} al día contra SAP para que ningún chofer pague nunca la
     * sincronización dentro de su request.
     *
     * <h3>Qué problema arregla (medido en producción el 19/08/2026, 14:12)</h3>
     * {@code SincronizacionEntregasService} ya evitaba el costo <i>repetido</i>, pero no el
     * primero: al arrancar, {@code inicializar()} deja el reloj vencido a propósito, así que el
     * primer chofer que abre la app encuentra el intervalo vencido, gana el candado y sincroniza
     * por todos. Con la JVM y el buffer pool de SQL Server en frío eso costó <b>6012 ms</b> —casi
     * 3x el peor caso medido en caliente (2129 ms)—, el request llegó a ~24 s en total y la app
     * del chofer cortó la conexión antes de recibir el JSON. En el log quedó la firma exacta de
     * ese caso: {@code [ok=1, fallidas=0, omitidas por intervalo=0, omitidas por candado=0]}, o
     * sea la primerísima sincronización del proceso.
     *
     * <p>Con este job, a los 30 s del arranque el scheduler paga esos segundos y a partir de ahí
     * los choferes siempre encuentran el intervalo fresco.
     *
     * <h3>Los tres números no son arbitrarios</h3>
     * <ul>
     *   <li><b>{@code fixedDelay} y no {@code fixedRate}.</b> {@code fixedDelay} cuenta desde que
     *       <i>terminó</i> la corrida anterior, así que una sincronización lenta nunca se solapa
     *       ni deja ejecuciones encoladas. Con {@code fixedRate}, un SAP degradado acumularía
     *       corridas atrasadas.</li>
     *   <li><b>120 s contra un intervalo de 180.</b> El período tiene que ser MENOR que
     *       {@code entregas.sync.intervalo-segundos}; si no, queda una ventana en la que el
     *       intervalo vence antes de la próxima corrida y vuelve a pagar un chofer.</li>
     *   <li><b>{@code initialDelay} de 30 s.</b> Para no pelearle el arranque al resto del
     *       contexto de Spring.</li>
     * </ul>
     *
     * <p>No hace falta try/catch: {@code sincronizarAhora()} no lanza nunca y ya loguea el
     * resultado. Tampoco puede solaparse con un request de chofer — comparten el mismo candado.
     */
    @Scheduled(fixedDelay = 120000L, initialDelay = 30000L)
    public void sincronizarEntregasConSap() {
        this.sincronizacionEntregasService.sincronizarAhora("programada");
    }

    /**
     * Este método se ejecuta cada día a las 23:58:59.
     *
     * <p>Cierra las entregas que el chofer dejó abiertas (ACCIÓN "C"). No manda ningún
     * aviso: el WhatsApp de una entrega sale en el momento en que se la marca como
     * entregada, no al cierre del día.
     */
    @Scheduled(cron = "59 58 23 * * *")
    public void executeDailyStoredProcedure() {
        try {
            logger.info("Iniciando ejecución del procedimiento almacenado ::: p_abm_trch_Entregas");
            this.entregaChoferDao.registrarEntregaChofer(new EntregaChofer(), "C");
            logger.info("Procedimiento almacenado ejecutado exitosamente ::: p_abm_trch_Entregas");
        } catch (Exception e) {
            logger.error("Error al ejecutar el procedimiento p_abm_trch_Entregas: {}", e.getMessage(), e);
        }
    }

    /**
     * Notifica los cumpleaños del día a las 08:00 am.
     * Obtiene la lista completa de empleados con fecha de cumpleaños y filtra
     * los que cumplen años hoy (mismo día y mes).
     */
    @Scheduled(cron = "0 0 8 * * *")
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
                return;
            }

            StringBuilder mensaje = new StringBuilder();
            mensaje.append("🥳 *Hoy es el cumpleaños de:*\n\n");
            for (Empleado emp : cumpleaneros) {
                String nombre = emp.getPersona().getDatoPersona() != null
                        ? emp.getPersona().getDatoPersona() : "Sin nombre";
                String sucursal = (emp.getSucursal() != null && emp.getSucursal().getNombre() != null)
                        ? emp.getSucursal().getNombre() : "";
                mensaje.append("🎂 ").append(nombre);
                if (!sucursal.isEmpty()) {
                    mensaje.append(" — ").append(sucursal);
                }
                mensaje.append("\n");
            }
            mensaje.append("\nTe deseamos un excelente día y un año lleno de éxitos personales y profesionales. 🎁🎉");

            whatsAppService.enviarNotificacionAGrupos("Cumpleaños", mensaje.toString());
            logger.info("Notificación de cumpleaños enviada para {} empleado(s)", cumpleaneros.size());

        } catch (Exception e) {
            logger.error("Error al notificar cumpleaños del día: {}", e.getMessage(), e);
        }
    }

    /**
     * Crea el rol de sábados del año que viene, en BORRADOR, del 22 al 31 de
     * diciembre a las 06:40.
     *
     * <p><b>La ventana es el cron y no un {@code if} en Java.</b> "Diez días antes
     * de que se acabe el año" es literalmente 22-31/12, y dejarlo en la expresión
     * mantiene la regla en un solo lugar legible. Además, al no poder dispararse
     * nunca en enero, el {@code +1} de adentro no puede equivocarse aunque el reloj
     * del servidor tenga drift.
     *
     * <p><b>Dispara diez veces y nueve no hace nada.</b> La idempotencia se lee acá
     * arriba: si ya hay un rol de ese año, sale por el primer {@code return} — sin
     * tocar la base y sin mandar WhatsApp. El SP tiene la misma guarda adentro (y el
     * UNIQUE de {@code trs_Rol} atrás de todo, por si dos instancias del backend
     * disparan juntas), así que ni siquiera hay una carrera que perder.
     *
     * <p>Se vuelve a preguntar DESPUÉS de llamar al SP porque el wrapper no devuelve
     * "lo creé" o "ya estaba": la evidencia es el rol mismo. Esa segunda consulta es
     * la que decide si se avisa, y de paso trae los contadores para el mensaje.
     *
     * <p>Nunca relanza: si el 22 a las 06:40 la base no responde, el hilo del
     * scheduler no se muere y el 23 se vuelve a intentar. Y si los diez días el
     * servidor estuvo apagado, la ventana no se pierde — queda la varita de RR.HH.
     * (que ya viene apuntando al año siguiente) y
     * {@code POST /scheduler/generar-rol-gestion-siguiente}.
     *
     * <p>06:40 no choca con los otros dos jobs (08:00 cumpleaños, 23:58:59 cierre).
     */
    @Scheduled(cron = "0 40 6 22-31 12 *")
    public void generarRolGestionSiguiente() {
        int anio = LocalDate.now().getYear() + 1;
        logger.info("Verificando el rol de sábados de la gestión {}", anio);
        try {
            List<Rol> existentes = rolDao.obtenerRoles(0, anio);
            if (existentes != null && !existentes.isEmpty()) {
                logger.info("El rol de la gestión {} ya existe ({} rol/es); no se genera nada.",
                            anio, existentes.size());
                return;
            }

            procesoRolDao.generarGestionSiguiente(anio, null);

            List<Rol> creados = rolDao.obtenerRoles(0, anio);
            if (creados == null || creados.isEmpty()) {
                // El SP decidió no hacer nada: no existe el rol del año anterior del
                // cual heredar. No es un incidente, pero conviene que alguien lo vea.
                logger.warn("No se creó el rol de la gestión {}: no hay ningún rol del {} "
                          + "para heredar. Si el módulo tiene que arrancar, hay que crear "
                          + "el primer rol a mano.", anio, anio - 1);
                return;
            }

            StringBuilder mensaje = new StringBuilder();
            mensaje.append("📅 *Rol de sábados ").append(anio).append(" generado*\n\n");
            for (Rol r : creados) {
                mensaje.append("• ").append(r.getNombre() != null ? r.getNombre() : "ROL " + anio);
                if (r.getSucursal() != null && !r.getSucursal().isEmpty()) {
                    mensaje.append(" — ").append(r.getSucursal());
                }
                mensaje.append(": ").append(r.getSabados()).append(" sábados, ")
                       .append(r.getParticipantes()).append(" participantes\n");
            }
            mensaje.append("\nQuedó en *BORRADOR*: todavía no es oficial y nadie lo ve publicado.\n")
                   .append("• Los grupos A/B y las exclusiones se heredaron del ")
                   .append(anio - 1).append("; conviene revisarlos.\n")
                   .append("• Si entra o sale gente después de hoy, hay que REGENERAR antes de publicar.\n")
                   .append("• Mientras siga en borrador, cualquiera puede regenerar y mover los sábados de todos.");

            whatsAppService.enviarNotificacionAGrupos("Rol de sábados", mensaje.toString());
            logger.info("Rol de la gestión {} creado: {} rol/es en BORRADOR.", anio, creados.size());

        } catch (Exception e) {
            logger.error("Error al generar el rol de la gestión {}: {}", anio, e.getMessage(), e);
        }
    }
}