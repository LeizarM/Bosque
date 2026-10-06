// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\TraspasoVerificadoDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Una fila de tac_traspasoMovCaja con su flag "¿Fue Verificado?" — ver ConfirmarCierreOperacionesRequest. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TraspasoVerificadoDto {
    private long idTrasp;
    private int fueVerificado;
}
