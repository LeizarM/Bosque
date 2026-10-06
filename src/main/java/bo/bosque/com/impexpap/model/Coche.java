// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\Coche.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Coche implements Serializable {

    // ── campos de la tabla tac_coche ─────────────────────────────────────────
    // Solo idCoche (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Integer envoltorio: p_abm_tac_Coche/p_list_tac_Coche los declaran
    // todos NULL-ables (filtros opcionales). audUsuario se queda como int
    // primitivo (no long): en esta tabla la columna es INT, no BIGINT, a
    // diferencia del resto del módulo. Hallazgo de code-review, 2026-09-02.
    private long idCoche;
    private String marca;
    private String clase;
    private String placa;
    private Integer anio;
    private String color;
    private Integer codSucursal;
    private Integer estado;
    private int audUsuario;
    private Date audFecha;
}
