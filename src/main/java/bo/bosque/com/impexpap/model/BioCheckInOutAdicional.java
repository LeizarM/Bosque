package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Marcación adicional/corregida (olvidada) — tabla {@code tbio_bioCHECKINOUTAdicinal}.
 *
 * <p>SPs: {@code p_abm_BioCHECKINOUTAdicinal} (I/U/D) · {@code p_list_BioCHECKINOUTAdicinal} (L).
 * Nombre de tabla/SP con el typo original ("Adicinal") — se conserva tal cual porque
 * es el nombre real en la base.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BioCheckInOutAdicional implements Serializable {

    private int USERID;
    private Date CHECKTIME;
    private int CODEMPLEADO;
    private String fechaString;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;
}
