// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\MarcarLlegadaCocheRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Cuerpo de POST /tareas-rutinarias/coches/marcar-llegada. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MarcarLlegadaCocheRequest {
    private long idCo;
    /** 0=NO, 1=SI. */
    private int llego;
    private String obs;
}
