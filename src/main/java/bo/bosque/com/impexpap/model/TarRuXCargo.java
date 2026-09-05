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
    private long idTarXCargo;
    private long idTarRuti;
    private long codCargo;
    private int estado;
    private long audUsuario;
    private Date audFecha;
    // Long, no long: NULL es un valor real y documentado acá ("aplica al
    // cargo en TODAS sus sucursales"), no la ausencia de dato — un primitivo
    // hace que BeanPropertyRowMapper reviente al mapear cualquier fila NULL,
    // y que Jackson silencie un null entrante a 0 (que en cambio SÍ es un
    // codSucursal real, así que no es lo mismo que "todas"). Encontrado en
    // code-review, 2026-09-02.
    private Long codCargoSucursal;
    private Date fechaInicio;
    private Date fechaFin;
}
