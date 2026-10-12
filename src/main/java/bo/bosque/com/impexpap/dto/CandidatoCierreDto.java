// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\CandidatoCierreDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * A quién se le puede asignar una sección del cierre
 * ({@code p_list_tac_ResponsableCierre 'C'}): un cargo en su sucursal (tipo C)
 * o una persona (tipo P), con si ya tiene la tarea y cuántas secciones.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CandidatoCierreDto {
    private String tipo;
    private Long codCargoSucursal;
    private Long codEmpleado;
    private String cargo;
    private String sucursal;
    private String personas;
    private Integer tieneLaTarea;
    private Integer secciones;
}
