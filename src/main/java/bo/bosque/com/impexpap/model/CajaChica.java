// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\CajaChica.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CajaChica implements Serializable {

    // ── campos de la tabla tac_cajaChica ────────────────────────────────────
    // Solo idCC (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Integer/Double envoltorio: p_abm_tac_CajaChica/p_list_tac_CajaChica
    // los declaran todos NULL-ables (filtros opcionales). Hallazgo de
    // code-review, 2026-09-02.
    private long idCC;
    private Long idBitTarRuti;
    private String persona;
    private Double montoIng;
    private Double montoEg;
    private Double saldo;
    private Integer numFactura;
    private Integer numVale;
    private String moneda;
    private String descripcion;
    private Long codEmpDestino;
    private Date fecha;
    private Long codSucursal;
    private Long lote;
    private long audUsuario;
    private Date audFecha;
}
