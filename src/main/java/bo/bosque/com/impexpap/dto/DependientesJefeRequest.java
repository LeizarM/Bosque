// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\DependientesJefeRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Cuerpo del endpoint que lista los dependientes del cargo vigente del usuario autenticado. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DependientesJefeRequest {

    /** "T" = todo el subárbol, "D" = solo reportes directos. Default "T" si viene null. */
    private String profundidad = "T";

    /** "A" = todas las sucursales, "M" = solo la sucursal del jefe. Default "A" si viene null. */
    private String alcanceSucursal = "A";
}
