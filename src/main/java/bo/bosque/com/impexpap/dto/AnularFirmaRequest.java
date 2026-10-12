// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\AnularFirmaRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Anular una firma de Verificar Cierre; el motivo lo exige el SP (archivo SQL 81). */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AnularFirmaRequest {
    private long idFC;
    private String obs;
}
