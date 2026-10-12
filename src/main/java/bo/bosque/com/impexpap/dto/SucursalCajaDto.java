// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\SucursalCajaDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Una sucursal con caja: arqueos, llegadas o arqueos asignados ({@code p_list_tac_ResponsableCierre 'S'}). */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SucursalCajaDto {
    private Long codSucursal;
    private String nombreSucursal;
}
