package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Horario semanal con nombre (compuesto por 7 filas de
 * {@code tbio_bioHrSemanalDetalle}) — tabla {@code tbio_bioHrSemanal}.
 *
 * <p>SPs: {@code p_abm_BioHrSemanal} (I/U/D) · {@code p_list_BioHrSemanal} (L).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioHrSemanal implements Serializable {

    private long idHrSemanal;
    private String nombre;
    private String estado;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;
}
