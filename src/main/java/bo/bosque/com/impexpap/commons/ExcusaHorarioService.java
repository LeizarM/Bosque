package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IAsignacion;
import bo.bosque.com.impexpap.dao.IBioEmplBosqEmpl;
import bo.bosque.com.impexpap.dao.IBioHrEmpleado;
import bo.bosque.com.impexpap.dao.IBioHrSemanalDetalle;
import bo.bosque.com.impexpap.dao.IBioHrs;
import bo.bosque.com.impexpap.dao.IEstadoTurno;
import bo.bosque.com.impexpap.dao.IParticipante;
import bo.bosque.com.impexpap.dao.ISabado;
import bo.bosque.com.impexpap.dto.ExcusaHorarioDto;
import bo.bosque.com.impexpap.model.Asignacion;
import bo.bosque.com.impexpap.model.BioEmplBosqEmpl;
import bo.bosque.com.impexpap.model.BioHrEmpleado;
import bo.bosque.com.impexpap.model.BioHrSemanalDetalle;
import bo.bosque.com.impexpap.model.BioHrs;
import bo.bosque.com.impexpap.model.EstadoTurno;
import bo.bosque.com.impexpap.model.Participante;
import bo.bosque.com.impexpap.model.Sabado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b>El biométrico (tbio_) pisa al Rol de Sábados (trs_).</b> Excusa (celda {@code 'E'})
 * a quien ya cumplió su cuota semanal de horas ANTES del sábado, porque esa semana tuvo
 * un horario distinto al suyo de siempre (p.ej. "Horario Extendido") — un horario
 * rotativo, "de acuerdo a lo que se presente en la vida real", no una excepción que
 * alguien cargue a mano.
 *
 * <p><b>Vive en {@code commons} y no en {@code RolSabadosController}</b> por si hiciera
 * falta otro disparador el día de mañana — hoy sólo lo llama
 * {@code POST /rol-sabados/refrescar-excusas-horario}, automático desde el Flutter
 * (ver {@code aplicarExcusasHorarioAlEntrarProvider}, que lo dispara solo apenas alguien
 * entra al módulo, sin botón y sin job de backend — así lo pidió el usuario el 04/09/2026:
 * primero un {@code @Scheduled} de madrugada, después "que sea en cuanto entre al módulo,
 * ese job no es necesario"). Separarlo de todos modos evita que un futuro segundo
 * disparador termine repitiendo estas ~180 líneas.
 *
 * <p><b>La cuota es personal, no un número fijo de la empresa:</b> la del horario BASE de
 * cada uno — el PRIMERO que se le asignó en {@code tbio_bioHrEmpleado} — que es la
 * referencia contra la que se mide cualquier horario rotativo posterior. Cuota = suma de
 * {@code tbio_bioHrs.cantMinutos} de los 7 días de ese horario base (o sea, YA incluye el
 * sábado que normalmente le toca). Si el horario vigente ESA semana (resuelto día por día,
 * igual que {@code BiometricoController.horarioVigente}) ya suma, de Lunes a Viernes, tanto
 * o más que esa cuota, el sábado sobra.
 *
 * <p><b>Guardas, para no pisar una decisión humana ni inventar excepciones donde no
 * corresponde:</b>
 * <ul>
 *   <li>Sólo sábados <b>sin evento</b> ({@code alcanceEvento IS NULL}) — un evento se
 *       maneja con {@code /convocar}, no con esto.</li>
 *   <li>Sólo si la celda de HOY es {@code '1'} con {@code origen='G'} — la rotación por
 *       defecto. Si ya es {@code 'L'/'M'/'P'} o no existe fila, alguien (persona o proceso)
 *       ya decidió algo distinto y esto no lo toca.</li>
 *   <li>Sólo empleados enlazados en {@code tbio_bioEmplBosqEmpl} CON horario asignado
 *       ({@code tbio_bioHrEmpleado}) — a quien no está en Biométrico (típicamente almacén)
 *       esto no le cambia nada, sigue la rotación normal.</li>
 *   <li>Sólo si el horario vigente esa semana es DISTINTO del horario base — sin rotación
 *       no hay nada que evaluar.</li>
 * </ul>
 *
 * <p><b>Por qué escribe {@code 'E'} y no {@code 'L'}:</b> {@code trs_sp_corregirCelda}
 * trata {@code 'L'} como "borrar la fila", sin ningún rastro de la decisión — para una
 * corrección humana puntual está bien, pero acá el motivo lo calculó el sistema y tiene
 * que quedar auditable. Por eso {@code p_list_Permiso @ACCION='N1'} (que decide si el
 * reporte biométrico marca un sábado como SABADO_LIBRE en vez de FALTA) tuvo que ampliarse
 * — ver {@code sql/18_biometrico_excusa_horas.sql}: antes sólo miraba "no existe fila";
 * ahora también cuenta una fila {@code 'E'}. Sin ese cambio, esto dejaría al empleado
 * excusado acá pero marcado FALTA en el biométrico.
 *
 * <p><b>Si alguien de verdad tiene que venir pese a haber cumplido la cuota</b> (un caso
 * excepcional), RR.HH. lo corrige a mano con el editor de celda de siempre — poniéndola en
 * {@code '1'} o convocándola a un evento. No hace falta una pantalla especial para
 * "deshacer" esto: es la misma corrección manual que ya existía para cualquier otra celda.
 */
@Slf4j
@Component
public class ExcusaHorarioService {

    private final ISabado sabadoDao;
    private final IParticipante participanteDao;
    private final IAsignacion asignacionDao;
    private final IEstadoTurno estadoTurnoDao;
    private final IBioEmplBosqEmpl bioEmplBosqEmplDao;
    private final IBioHrEmpleado bioHrEmpleadoDao;
    private final IBioHrSemanalDetalle bioHrSemanalDetalleDao;
    private final IBioHrs bioHrsDao;

    public ExcusaHorarioService(ISabado sabadoDao,
                                 IParticipante participanteDao,
                                 IAsignacion asignacionDao,
                                 IEstadoTurno estadoTurnoDao,
                                 IBioEmplBosqEmpl bioEmplBosqEmplDao,
                                 IBioHrEmpleado bioHrEmpleadoDao,
                                 IBioHrSemanalDetalle bioHrSemanalDetalleDao,
                                 IBioHrs bioHrsDao) {
        this.sabadoDao = sabadoDao;
        this.participanteDao = participanteDao;
        this.asignacionDao = asignacionDao;
        this.estadoTurnoDao = estadoTurnoDao;
        this.bioEmplBosqEmplDao = bioEmplBosqEmplDao;
        this.bioHrEmpleadoDao = bioHrEmpleadoDao;
        this.bioHrSemanalDetalleDao = bioHrSemanalDetalleDao;
        this.bioHrsDao = bioHrsDao;
    }

    /**
     * Recorre participantes activos × sábados sin evento del rol; para cada par que
     * cumple las guardas (documentadas en la clase) compara horas y, si corresponde, arma
     * la fila del informe y — salvo {@code soloInformar} — escribe la celda.
     *
     * <p>Las consultas caras se cachean UNA vez por rol/horario, no por par
     * participante×sábado: las celdas actuales se traen una vez por sábado (no una vez por
     * persona) y el detalle de cada horario semanal se resuelve una sola vez aunque lo
     * compartan 40 empleados — mismo patrón que {@code BiometricoController.calcularReporte}
     * con sus mapas {@code computeIfAbsent}.
     *
     * @param codEmpleadoEjecutor quién firma la escritura ante {@code p_abm_trs_Asignacion}
     *                            — null en una corrida automática (sin un empleado detrás).
     * @param esAdmin             1 = bypassa la validación de RR.HH./jefe del SP. El botón
     *                            manual lo resuelve del token de quien lo aprieta; el job
     *                            automático manda 1 siempre — es el sistema actuando con
     *                            la autoridad que RR.HH. ya le dio al crear esta regla.
     * @param audUsuario          quién queda en la auditoría de la celda; null en una
     *                            corrida automática, igual que
     *                            {@code DatabaseTaskScheduler.generarRolGestionSiguiente}
     *                            manda {@code null} en {@code generarGestionSiguiente}.
     */
    public List<ExcusaHorarioDto> calcular(long idRol, boolean soloInformar,
                                            Long codEmpleadoEjecutor, int esAdmin, Long audUsuario) {
        List<ExcusaHorarioDto> resultado = new ArrayList<>();

        Map<Integer, String> letraPorEstado = new HashMap<>();
        for (EstadoTurno et : estadoTurnoDao.obtenerEstadosTurno()) {
            letraPorEstado.put(et.getIdEstadoTurno(), et.getCodigoExcel());
        }

        LocalDate hoy = LocalDate.now();
        List<Sabado> sabados = sabadoDao.obtenerSabados(idRol).stream()
                .filter(s -> s.getActivo() == 1 && s.getAlcanceEvento() == null
                        && !toLocalDate(s.getFecha()).isBefore(hoy))
                .collect(Collectors.toList());
        if (sabados.isEmpty()) return resultado;

        // Celdas de esos sábados, UNA consulta por sábado (no por participante), indexadas
        // por idParticipante para lookup en memoria adentro del loop de abajo.
        Map<Long, Map<Long, Asignacion>> celdasPorSabado = new HashMap<>();
        for (Sabado s : sabados) {
            Map<Long, Asignacion> porParticipante = new HashMap<>();
            for (Asignacion a : asignacionDao.obtenerAsignaciones(idRol, s.getIdSabado())) {
                porParticipante.put(a.getIdParticipante(), a);
            }
            celdasPorSabado.put(s.getIdSabado(), porParticipante);
        }

        Map<Long, List<BioHrSemanalDetalle>> detallePorSemanal = new HashMap<>();
        Map<Long, BioHrs> hrsPorId = new HashMap<>();

        for (Participante p : participanteDao.obtenerParticipantes(idRol, 1)) {
            List<BioEmplBosqEmpl> cruce = bioEmplBosqEmplDao.listar(mapa("idEmpleado", p.getCodEmpleado()));
            if (cruce.stream().noneMatch(c -> c.getIdEmpleado() > 0)) continue; // no enlazado al biométrico

            List<BioHrEmpleado> asignacionesBio = bioHrEmpleadoDao.listar(mapa("idEmplead", p.getCodEmpleado()));
            if (asignacionesBio.isEmpty()) continue; // nunca le pusieron horario

            // El horario BASE: el primero que se le asignó — confirmado con el usuario
            // (04/09/2026): con horarios rotativos, la referencia es la que se presentó
            // primero en la vida real, no la más antigua "vigente hoy" ni una marcada a mano.
            BioHrEmpleado base = asignacionesBio.stream()
                    .filter(a -> a.getInicio() != null)
                    .min(Comparator.comparing(BioHrEmpleado::getInicio))
                    .orElse(null);
            if (base == null) continue;

            double cuotaMin = minutosDelHorario(base.getIdHrSemanal(), detallePorSemanal, hrsPorId);
            if (cuotaMin <= 0) continue; // horario base sin turnos cargados: no hay con qué comparar

            for (Sabado s : sabados) {
                Asignacion celda = celdasPorSabado.get(s.getIdSabado()).get(p.getIdParticipante());
                if (celda == null) continue; // no le toca este sábado -> nada que excusar
                if (!"1".equals(letraPorEstado.get(celda.getIdEstadoTurno())) || !"G".equals(celda.getOrigen())) {
                    continue; // no es la rotación por defecto sin tocar: alguien ya decidió otra cosa
                }

                LocalDate fecha = toLocalDate(s.getFecha());
                LocalDate lunes = fecha.minusDays(5); // el sábado siempre cae 5 días después del lunes de su semana

                double sumaMin = 0;
                boolean huboRotacion = false;
                for (int i = 0; i < 5; i++) {
                    LocalDate dia = lunes.plusDays(i);
                    BioHrEmpleado vigente = horarioVigente(asignacionesBio, dia);
                    if (vigente == null) continue;
                    // Comparar por PLANTILLA (idHrSemanal), no por fila (idHrEmpleado).
                    // Confirmado en producción con MORALES CHIPANA EDWIN (04/09/2026):
                    // una alternancia semanal PERMANENTE entre dos horarios reasigna
                    // "ADM CONT 1" con un idHrEmpleado NUEVO cada vez que vuelve a
                    // tocarle (fila 3 la primera vez, fila 103 la siguiente...) aunque
                    // sea la MISMA plantilla que el horario base. Comparando por fila,
                    // esas semanas "de vuelta a la normalidad" se leían igual que una
                    // semana rotada de verdad -- sólo no rompía nada porque además
                    // sumaMin quedaba por debajo de la cuota, pero era casualidad, no
                    // la regla funcionando.
                    if (vigente.getIdHrSemanal() != base.getIdHrSemanal()) huboRotacion = true;
                    sumaMin += minutosDelDia(vigente.getIdHrSemanal(), dia.getDayOfWeek().getValue(),
                            detallePorSemanal, hrsPorId);
                }
                if (!huboRotacion || sumaMin < cuotaMin) continue;

                String motivo = String.format(
                        "Ya cumplió %.0f de sus %.0f minutos de cuota semanal, de Lunes a Viernes, "
                      + "por un horario distinto al suyo de siempre — no le toca este sábado.",
                        sumaMin, cuotaMin);

                ExcusaHorarioDto fila = new ExcusaHorarioDto();
                fila.setCodEmpleado(p.getCodEmpleado());
                fila.setNombreEmpleado(p.getNombreRol());
                fila.setIdParticipante(p.getIdParticipante());
                fila.setIdSabado(s.getIdSabado());
                fila.setFecha(s.getFecha());
                fila.setMinutosSemana(sumaMin);
                fila.setMinutosCuota(cuotaMin);
                fila.setMotivo(motivo);

                if (soloInformar) {
                    fila.setAplicado(false);
                } else {
                    try {
                        Asignacion mb = new Asignacion();
                        mb.setIdParticipante(p.getIdParticipante());
                        mb.setIdSabado(s.getIdSabado());
                        mb.setCodigoExcel("E");
                        mb.setObservacion(motivo.length() > 200 ? motivo.substring(0, 200) : motivo);
                        mb.setCodEmpleadoEjecutor(codEmpleadoEjecutor);
                        mb.setEsAdmin(esAdmin);
                        mb.setAudUsuario(audUsuario);
                        asignacionDao.registrarAsignacion(mb, "U");
                        fila.setAplicado(true);
                    } catch (SpBusinessException | DataAccessException ex) {
                        // Un fallo puntual (rol cerrado a mitad de la corrida, celda tocada
                        // por otro justo ahora) no aborta el resto del lote: se informa y sigue.
                        log.warn("No se pudo excusar por horario a codEmpleado={} en idSabado={}: {}",
                                p.getCodEmpleado(), s.getIdSabado(), ex.getMessage());
                        fila.setAplicado(false);
                        fila.setError(ex.getMessage());
                    }
                }
                resultado.add(fila);
            }
        }
        return resultado;
    }

    /** Suma de {@code cantMinutos} de TODOS los días (1-7) que tiene definidos ese horario semanal. */
    private double minutosDelHorario(long idHrSemanal, Map<Long, List<BioHrSemanalDetalle>> detallePorSemanal,
                                      Map<Long, BioHrs> hrsPorId) {
        double total = 0;
        for (int dia = 1; dia <= 7; dia++) {
            total += minutosDelDia(idHrSemanal, dia, detallePorSemanal, hrsPorId);
        }
        return total;
    }

    /** {@code cantMinutos} del turno que ese horario semanal tiene para UN día (0 si no tiene). */
    private double minutosDelDia(long idHrSemanal, int diaSemana, Map<Long, List<BioHrSemanalDetalle>> detallePorSemanal,
                                  Map<Long, BioHrs> hrsPorId) {
        List<BioHrSemanalDetalle> detalle = detallePorSemanal.computeIfAbsent(idHrSemanal,
                id -> bioHrSemanalDetalleDao.listar(mapa("idHrSemanal", id)));
        for (BioHrSemanalDetalle d : detalle) {
            if (d.getDia() == diaSemana && d.getIdHrs() > 0) {
                BioHrs turno = hrsPorId.computeIfAbsent(d.getIdHrs(), id -> {
                    List<BioHrs> r = bioHrsDao.listar(mapa("idHrs", id));
                    return r.isEmpty() ? null : r.get(0);
                });
                return turno != null ? turno.getCantMinutos() : 0;
            }
        }
        return 0;
    }

    /**
     * El horario vigente PARA ESE DÍA: el de {@code inicio} más reciente que no sea
     * posterior al día. Copia intencional de {@code BiometricoController.horarioVigente} —
     * es la misma regla ("temporal", día por día) que ya se usa para el reporte biométrico
     * y para la franja "Cómo se repartió" del front; este módulo no depende de esa clase,
     * así que se repite acá en vez de acoplar dos controladores por un método de cuatro
     * líneas.
     */
    private static BioHrEmpleado horarioVigente(List<BioHrEmpleado> asignaciones, LocalDate dia) {
        BioHrEmpleado mejor = null;
        LocalDate mejorInicio = null;
        for (BioHrEmpleado a : asignaciones) {
            if (a.getInicio() == null) continue;
            LocalDate inicio = toLocalDate(a.getInicio());
            if (!inicio.isAfter(dia) && (mejorInicio == null || inicio.isAfter(mejorInicio))) {
                mejor = a;
                mejorInicio = inicio;
            }
        }
        return mejor;
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Map<String, Object> mapa(String clave, Object valor) {
        Map<String, Object> m = new HashMap<>();
        m.put(clave, valor);
        return m;
    }
}
