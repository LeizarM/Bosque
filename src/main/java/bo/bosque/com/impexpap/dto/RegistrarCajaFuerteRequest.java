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
    // Ni ocurrencia ni sucursal (archivo SQL 96): el SP la saca del cargo
    // vigente del empleado del token.
    private List<LlegadaCajaFuerteDto> llegadas;
}
