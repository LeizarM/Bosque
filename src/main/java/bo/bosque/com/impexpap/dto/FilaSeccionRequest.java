// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\FilaSeccionRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Las filas de una celda con fuente de la 39, o la marca de una de ellas
 * (archivo SQL 86). Que la celda sea de quien pide lo revisa el SP.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FilaSeccionRequest {
    private long idBitTarea;
    private String seccion;
    /** Vacía = toda la empresa. */
    private Long codSucursal;
    /** Marcar: la fila. */
    private String clave;
    /** Marcar: 1 revisada, 0 quitar la marca. */
    private Integer marca;
    /** Leer: un día anterior al de la celda, solo para consultar (archivo SQL 92). */
    private Date fecha;
}
