package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Plantilla de turno/horario (hora de ingreso y salida) — tabla {@code tbio_bioHrs}.
 *
 * <p>SPs: {@code p_abm_BioHrs} (I/U/D) · {@code p_list_BioHrs} (L).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioHrs implements Serializable {

    private long idHrs;
    private String nombre;
    private Date ingreso;
    private Date salida;
    private double cantDias;
    private double cantMinutos;
    private String estado;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;
}
