// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\DocumentacionArqueoDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Una fila de monto por tipo de documentación dentro del arqueo (idATR=2). */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DocumentacionArqueoDto {
    private long idDoc;
    private double monto;
}
