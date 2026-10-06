// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\AccionTareaRutinaria.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AccionTareaRutinaria implements Serializable {

    // ── campos de la tabla tac_accionTareaRutinaria ─────────────────────────
    // "accion" (columna real) se expone como "descripcionAccion" para no
    // colisionar con el parámetro @ACCION del SP (ver tac_accionTareaRutinaria.sql).
    // Solo idATR (PK) y audUsuario son NOT NULL de verdad — estado es
    // Integer envoltorio, no primitivo: p_abm_tac_AccionTareaRutinaria/
    // p_list_tac_AccionTareaRutinaria lo declaran NULL-able (filtro
    // opcional). Hallazgo de code-review, 2026-09-02.
    private long idATR;
    private String descripcionAccion;
    private Integer estado;
    private long audUsuario;
    private Date audFecha;
}
