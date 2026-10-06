// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\Frecuencia.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Frecuencia implements Serializable {

    // ── campos de la tabla tac_frecuencia ───────────────────────────────────
    // Solo idFrec (PK) y audUsuario son NOT NULL de verdad — estado es
    // Integer envoltorio: p_abm_tac_Frecuencia/p_list_tac_Frecuencia lo
    // declaran NULL-able (filtro opcional). Hallazgo de code-review,
    // 2026-09-02.
    private long idFrec;
    private String descripcion;
    private Integer estado;
    private long audUsuario;
    private Date audFecha;
}
