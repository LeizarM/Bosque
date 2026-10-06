// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\Corte.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Corte implements Serializable {

    // ── campos de la tabla tac_corte ────────────────────────────────────────
    // Solo idCorte (PK) y audUsuario son NOT NULL de verdad — corte es
    // Double envoltorio: p_abm_tac_Corte/p_list_tac_Corte lo declaran
    // NULL-able (filtro opcional). Hallazgo de code-review, 2026-09-02.
    private long idCorte;
    private String descripcion;
    private Double corte;
    private String tipoCorte;
    private long audUsuario;
    private Date audFecha;
}
