// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\FuenteCierreRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Registrar, cambiar, activar o borrar una fuente del cierre. Las reglas
 * (que exista, que se llame fn_tac_cierre_..., que nadie la use al borrarla)
 * las revisa {@code p_abm_tac_ResponsableCierre} F, A y B (archivo SQL 87).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FuenteCierreRequest {
    private String fuente;
    private String nombre;
    private String descripcion;
    private String spMarcar;
    /** A: 0 la desactiva, 1 la activa. */
    private Integer estado;
}
