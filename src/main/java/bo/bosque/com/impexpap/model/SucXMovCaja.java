// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\SucXMovCaja.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SucXMovCaja implements Serializable {

    // ── campos de la tabla tac_sucXMovCaja ──────────────────────────────────
    // Solo idSxMC (PK) y audUsuario son NOT NULL de verdad — codSucursal es
    // Long envoltorio: p_abm_tac_SucXMovCaja/p_list_tac_SucXMovCaja lo
    // declaran NULL-able (filtro opcional). Hallazgo de code-review,
    // 2026-09-02.
    private long idSxMC;
    private Long codSucursal;
    private String codigoCuentaCajaSap;
    private String nombre;
    private String bd;
    private long audUsuario;
    private Date audFecha;
}
