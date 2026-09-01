package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Una fila del resumen mensual de asistencia — un empleado, un mes.
 * Columnas alineadas con el legacy ({@code RptBiometricoResumenPorEmpleadoMensual},
 * "Empleado / Días Asignados / Días NO Marcados / Atrasos / Observaciones"), pero con los
 * valores corregidos: {@link #diasNoMarcados} y {@link #minutosAtraso} ya excluyen los días
 * que no eran una obligación real (feriado, sábado que no le tocaba, permiso, vacación) —
 * el legacy los contaba como falta/atraso igual. Ver
 * {@code BiometricoController.calcularFilaResumen}.
 */
@Getter
@Setter
public class ResumenAsistenciaEmpleadoDto {
    private long codEmpleado;
    private String nombreEmpleado;

    /** Días del mes con horario asignado (turno != null) — incluye feriados/permisos, no sólo los trabajados. */
    private int diasAsignados;

    /** Días realmente sin marcar (estado FALTA) — ya sin los sábados que no le tocaban ni feriados/permisos. */
    private int diasNoMarcados;

    /** Suma de {@link AsistenciaDiaDto#getMinutosAtraso()} del mes — 0 en los días que no eran obligación real. */
    private int minutosAtraso;

    /** Resumen corto de por qué hay días "sin exigencia" (feriados, permisos, sábados libres...), o {@code null} si no hay ninguno. */
    private String observaciones;
}
