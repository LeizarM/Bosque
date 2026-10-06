// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\RegistrarCajaFuerteRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.List;

/** Cuerpo de POST /tareas-rutinarias/caja-fuerte/registrar. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarCajaFuerteRequest {
    private long idTarRuti;
    private long idBitTarea;
    // codSucursal ya NO viaja aquí — el proc lo resuelve del cargo vigente
    // del empleado dueño de la ocurrencia (paso 20, code-review 2026-09-03).
    private List<LlegadaCajaFuerteDto> llegadas;
}
