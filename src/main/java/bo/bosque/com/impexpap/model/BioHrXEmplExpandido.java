package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Calendario expandido día por día (horario esperado por empleado y fecha) —
 * tabla {@code tbio_bioHrXEmplExpandido}.
 *
 * <p>SPs: {@code p_abm_BioHrXEmplExpandido} (I/U/D, más {@code ACCION='A'/'B'/'C'}
 * para (re)generar un rango — ver {@link bo.bosque.com.impexpap.dao.IBioHrXEmplExpandido})
 * · {@code p_list_BioHrXEmplExpandido} (L).
 *
 * <p><b>Bug conocido en {@code ACCION='A'} (a corregir, no a portar tal cual):</b> el SP
 * elige el {@code idHrEmpleado} aplicable con {@code TOP(1) ... ORDER BY inicio DESC}
 * por EMPLEADO, no por día — así que si un empleado tiene horarios distintos dentro
 * del mismo mes (varias filas en {@code tbio_bioHrEmpleado} con {@code inicio}
 * distinto), el mes completo termina expandido con el horario de {@code inicio} más
 * reciente, ignorando los tramos anteriores. Ver {@code CLAUDE.md} de este proyecto.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioHrXEmplExpandido implements Serializable {

    private long idHrEmpleado;
    private long idHrs;
    private Date jornada;
    private int dia;
    private Date hrIngreso;
    private Date hrSalida;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;
}
