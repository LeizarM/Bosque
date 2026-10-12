// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\ResponsableCierreDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Una fila de la matriz de Verificar Cierre ({@code p_list_tac_ResponsableCierre
 * 'L'}, archivo SQL 81): un cargo en su sucursal o una persona revisa una
 * sección, de una sucursal o de todas.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ResponsableCierreDto {
    private Long idRC;
    private Long idTarRuti;
    private String seccion;
    private String nombreSeccion;
    /** 1 si la sección se pide hoy (archivo SQL 86). */
    private Integer seccionVigente;
    private Long codSucursal;
    private String nombreSucursal;
    private Long codCargoSucursal;
    private String cargo;
    private String sucursalDelCargo;
    private Long codEmpleado;
    /** Quién ocupa hoy ese cargo, o la persona asignada. */
    private String personas;
    private Integer puedeAutorrevisar;
    private Integer estado;
}
