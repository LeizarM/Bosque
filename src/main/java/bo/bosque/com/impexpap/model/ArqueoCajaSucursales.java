// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\ArqueoCajaSucursales.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArqueoCajaSucursales implements Serializable {

    // ── campos de la tabla tac_arqueoCajaSucursales ─────────────────────────
    // Solo idAC (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Double envoltorio: p_abm_tac_ArqueoCajaSucursales/
    // p_list_tac_ArqueoCajaSucursales los declaran todos NULL-ables (filtros
    // opcionales). Hallazgo de code-review, 2026-09-02.
    private long idAC;
    private Long idTarRuti;
    private Long codEmpleadoEncargado;
    private Date fecha;
    private String hora;
    private Double saldoMovSap;
    private String obs;
    private Long codEmpleadoSupCierre;
    private Double total;
    private Double diferencia;
    private Double tc;
    private Long idBitTarea;
    private Date fechaRevisado;
    private long audUsuario;
    private Date audFecha;
}
