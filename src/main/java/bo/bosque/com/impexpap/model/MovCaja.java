// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\MovCaja.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MovCaja implements Serializable {

    // ── campos de la tabla tac_movCaja ──────────────────────────────────────
    // Solo idMC (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Double envoltorio: p_abm_tac_MovCaja/p_list_tac_MovCaja los
    // declaran todos NULL-ables (filtros opcionales). Hallazgo de
    // code-review, 2026-09-02.
    private long idMC;
    private Long idAC;
    private Long idSxMC;
    private Long codSucursal;
    private Double montoBs;
    private long audUsuario;
    private Date audFecha;
}
