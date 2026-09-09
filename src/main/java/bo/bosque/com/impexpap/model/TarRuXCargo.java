// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\TarRuXCargo.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TarRuXCargo implements Serializable {

    // ── campos de la tabla tac_tarRuXCargo ─────────────────────────────────
    // Solo idTarXCargo (PK) y audUsuario son NOT NULL de verdad — el resto
    // son Long/Integer envoltorio: p_abm_tac_TarRuXCargo/p_list_tac_TarRuXCargo
    // los declaran todos NULL-ables. Mismo bug ya corregido en las 18 tablas
    // hermanas de este scaffold; encontrado aquí recién en el loop de
    // auditoría continua, 2026-09-07 (se había saltado en el pase original).
    private long idTarXCargo;
    private Long idTarRuti;
    private Long codCargo;
    private Integer estado;
    private long audUsuario;
    private Date audFecha;
    // Long, no long: NULL es un valor real y documentado aquí ("aplica al
    // cargo en TODAS sus sucursales"), no la ausencia de dato — un primitivo
    // hace que BeanPropertyRowMapper reviente al mapear cualquier fila NULL,
    // y que Jackson silencie un null entrante a 0 (que en cambio SÍ es un
    // codSucursal real, así que no es lo mismo que "todas"). Encontrado en
    // code-review, 2026-09-02.
    private Long codCargoSucursal;
    private Date fechaInicio;
    private Date fechaFin;
}
