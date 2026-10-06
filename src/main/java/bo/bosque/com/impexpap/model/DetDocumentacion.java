// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\DetDocumentacion.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DetDocumentacion implements Serializable {

    // ── campos de la tabla tac_detDocumentacion ─────────────────────────────
    // Solo idDetDoc (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Double envoltorio: p_abm_tac_DetDocumentacion/
    // p_list_tac_DetDocumentacion los declaran todos NULL-ables (filtros
    // opcionales). Hallazgo de code-review, 2026-09-02.
    private long idDetDoc;
    private Long idAC;
    private Long idDoc;
    private Double monto;
    private long audUsuario;
    private Date audFecha;
}
