// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\FirmarSeccionRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Firmar una celda de Verificar Cierre (archivo SQL 81). Todas las reglas
 * (filas sin marcar, hallazgos sin observación, autorrevisión) las valida
 * {@code p_abm_tac_FirmaCierre 'F'}; aquí no se revisa nada.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FirmarSeccionRequest {
    private long idBitTarea;
    private String seccion;
    /** Null para traspasos, cheques y tareas del día (toda la empresa). */
    private Long codSucursal;
    private String obs;
}
