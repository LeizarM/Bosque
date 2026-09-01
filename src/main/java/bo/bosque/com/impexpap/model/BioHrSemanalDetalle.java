package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Día de la semana (1–7) → plantilla de turno dentro de un horario semanal —
 * tabla {@code tbio_bioHrSemanalDetalle}.
 *
 * <p>SPs: {@code p_abm_BioHrSemanalDetalle} (I/U/D) · {@code p_list_BioHrSemanalDetalle} (L).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioHrSemanalDetalle implements Serializable {

    private long idHrDet;
    private long idHrSemanal;
    private long idHrs;
    private int dia;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;
}
