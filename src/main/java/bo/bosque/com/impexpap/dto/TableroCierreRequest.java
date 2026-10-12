// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\TableroCierreRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Rango del tablero de Verificar Cierre. Null = los últimos 7 días; el SP lo
 * acota a 93 días y a 'cierreMedirDesde' (archivo SQL 81).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TableroCierreRequest {
    private Date fechaIni;
    private Date fechaFin;
}
