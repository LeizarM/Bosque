// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\TraspasoEfectivoPendienteDto.java
package bo.bosque.com.impexpap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Date;

/**
 * Una transferencia de efectivo entre sistemas pendiente de verificar
 * ({@code p_list_TesBase @ACCION='B'}, del sistema anterior, sin cambios).
 *
 * <p>Es del subsistema TesBase (tesorería, {@code ttes_TesBase}). No confundir
 * con la verificación de Caja AXA ({@code tac_traspasoMovCaja}): tienen nombres
 * casi iguales y ya se confundieron una vez (archivo SQL 51).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TraspasoEfectivoPendienteDto {

    private long codTes;
    private String codCliente;
    private String datoCliente;

    /**
     * La empresa de origen. El SP la devuelve en una columna llamada
     * {@code nombre} (join a tb_empresa) y no se le cambia el alias porque el
     * procedimiento es del sistema anterior; se renombra solo hacia afuera.
     */
    @JsonProperty("nombreEmpresa")
    private String nombre;

    private Date fechaRegistro;
    private String observacion;
    private String estado;
    private Date fechaFinalizacion;
}
