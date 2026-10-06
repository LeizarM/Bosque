// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\CochesDelDiaRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Cuerpo de POST /tareas-rutinarias/coches/listar-del-dia. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CochesDelDiaRequest {
    private long idTarRuti;
    private long idBitTarea;
    // codSucursal ya NO viaja aquí — el proc lo resuelve del cargo vigente
    // del empleado dueño de la ocurrencia (paso 20, code-review 2026-09-03).
}
