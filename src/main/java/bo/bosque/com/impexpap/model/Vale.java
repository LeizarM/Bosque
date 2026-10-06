// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\Vale.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Vale implements Serializable {

    // ── campos de la tabla tac_vale ─────────────────────────────────────────
    // Solo idVale (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Integer/Double envoltorio: p_abm_tac_Vale/p_list_tac_Vale los
    // declaran todos NULL-ables (filtros opcionales). Hallazgo de
    // code-review, 2026-09-02.
    private long idVale;
    private Long idAC;
    private Integer numVale;
    private String nombre;
    private Double monto;
    private Date fecha;
    private String obs;
    private Integer codEmpresa;
    private long audUsuario;
    private Date audFecha;
}
