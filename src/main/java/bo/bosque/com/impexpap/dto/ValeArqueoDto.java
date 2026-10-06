// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\ValeArqueoDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/** Una fila de vale, agregada libremente dentro del arqueo (idATR=2). */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ValeArqueoDto {
    private Integer numVale;
    private String nombre;
    private Double monto;
    private Date fecha;
    private Integer codEmpresa;
    private String obs;
}
