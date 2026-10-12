// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\SenalesCargoRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Cargos por revisar (archivo SQL 82). Para listar: {@code verCalladas}.
 * "Está bien así": persona, tarea y una explicación opcional. Volver a
 * mostrar: {@code idSC} y la persona.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SenalesCargoRequest {
    private boolean verCalladas;
    private Long codEmpleado;
    private Long idTarRuti;
    private Long idSC;
    private String obs;
}
