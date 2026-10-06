// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\DetArqueoCajaSucursales.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DetArqueoCajaSucursales implements Serializable {

    // ── campos de la tabla tac_detArqueoCajaSucursales ─────────────────────
    // Solo idDetAS (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Integer/Double envoltorio: p_abm_tac_DetArqueoCajaSucursales/
    // p_list_tac_DetArqueoCajaSucursales los declaran todos NULL-ables
    // (filtros opcionales). Hallazgo de code-review, 2026-09-02.
    private long idDetAS;
    private Long idAC;
    private Long idCorte;
    private Integer cantidad;
    private Double subTotal;
    private long audUsuario;
    private Date audFecha;
}
