package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Cruce entre un usuario del reloj biométrico y un empleado de Bosque —
 * tabla {@code tbio_bioEmplBosqEmpl}.
 *
 * <p>SPs: {@code p_abm_BioEmplBosqEmpl} (I/U/D) · {@code p_list_BioEmplBosqEmpl} (L).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioEmplBosqEmpl implements Serializable {

    private long idEmpleadBio;
    private String datoNombreBiom;
    private long idEmpleado;
    private String datoNombreBosq;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;
}
