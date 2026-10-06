// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\CorteArqueoDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Una fila de conteo de denominación dentro del arqueo (idATR=2). */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CorteArqueoDto {
    private long idCorte;
    private int cantidad;
}
