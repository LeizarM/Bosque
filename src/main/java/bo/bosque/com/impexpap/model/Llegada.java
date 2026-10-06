// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\Llegada.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Llegada implements Serializable {

    // ── campos de la tabla tac_llegada ──────────────────────────────────────
    // Solo idRp (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Integer/Double envoltorio: p_abm_tac_Llegada/p_list_tac_Llegada
    // los declaran todos NULL-ables (filtros opcionales), y un primitivo ahí
    // hace que un 0 se mande como filtro real o reviente el mapeo de una
    // fila NULL. Hallazgo de code-review, 2026-09-02.
    private long idRp;
    private Long idTarRuti;
    private Date fecha;
    private Date horallegada;
    private String persona;
    private String cliente;
    private String moneda;
    private Double importe;
    private String destino;
    private String tipo;
    private String obs;
    private Long idBitTarRuti;
    private Integer fueVerificado;
    private Long codEmpVerificado;
    private Integer codSucursal;
    private long audUsuario;
    private Date audFecha;
}
