// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\MontoCajaChicaXSuc.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MontoCajaChicaXSuc implements Serializable {

    // ── campos de la tabla tac_montoCajaChicaXSuc ───────────────────────────
    // Solo idCS (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Double envoltorio: p_abm_tac_MontoCajaChicaXSuc/
    // p_list_tac_MontoCajaChicaXSuc los declaran todos NULL-ables (filtros
    // opcionales). Hallazgo de code-review, 2026-09-02.
    private long idCS;
    private Long codSucursal;
    private Double montoIng;
    private long audUsuario;
    private Date audFecha;
}
