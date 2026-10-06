// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\CajaChicaListarRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Cuerpo de POST /tareas-rutinarias/caja-chica/listar-del-lote. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CajaChicaListarRequest {
    private long idBitTarea;
    // codSucursal ya NO viaja aquí — el proc lo resuelve del cargo vigente
    // del empleado dueño de la ocurrencia (paso 20, code-review 2026-09-03).
}
