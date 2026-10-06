// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\CocheLlegadas.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CocheLlegadas implements Serializable {

    // ── campos de la tabla tac_cocheLlegadas ─────────────────────────────────
    // Solo idCo (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Integer envoltorio: p_abm_tac_CocheLlegadas/
    // p_list_tac_CocheLlegadas los declaran todos NULL-ables (filtros
    // opcionales). Hallazgo de code-review, 2026-09-02.
    private long idCo;
    private Long idTarRuti;
    private Long idCoche;
    private Long idBitTarRuti;
    private String descripcion;
    private Date fecha;
    private Integer llego;
    private String obs;
    private long audUsuario;
    private Date audFecha;
}
