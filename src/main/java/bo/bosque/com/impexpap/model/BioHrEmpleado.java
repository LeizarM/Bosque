package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Asignación de un horario semanal a un empleado desde una fecha —
 * tabla {@code tbio_bioHrEmpleado}.
 *
 * <p>SPs: {@code p_abm_BioHrEmpleado} (I/U/D) · {@code p_list_BioHrEmpleado} (L).
 *
 * <p>No tiene fecha de fin: una fila nueva con {@code inicio} posterior
 * reemplaza a la anterior desde esa fecha en adelante — un mismo empleado puede
 * tener N filas (N horarios) vigentes en distintos tramos del mismo mes. Ver
 * {@link bo.bosque.com.impexpap.dao.IBioHrXEmplExpandido} para cómo se expande
 * esto a un calendario día por día.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioHrEmpleado implements Serializable {

    private long idHrEmpleado;
    private long idHrSemanal;
    private long idEmplead;
    private Date inicio;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;
}
