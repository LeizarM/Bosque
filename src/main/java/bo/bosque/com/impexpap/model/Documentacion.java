// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\Documentacion.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Documentacion implements Serializable {

    // ── campos de la tabla tac_documentacion ───────────────────────────────
    private long idDoc;
    private String nombre;
    private long audUsuario;
    private Date audFecha;
}
