// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\SeccionCierreRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Alta, cambio, baja o vuelta de una sección del cierre. Las reglas (nombre,
 * fuente registrada, qué se puede cambiar en las propias) las revisa
 * {@code p_abm_tac_ResponsableCierre} 'S' y 'Q' (archivo SQL 86).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SeccionCierreRequest {
    /** Vacía = sección nueva. */
    private String seccion;
    private String nombre;
    private String queRevisar;
    private Integer porSucursal;
    private String fuente;
    private Integer marcaPorFila;
    private Integer firmaSola;
    private Integer obsDesde;
    private String diaRevisado;
    private Integer orden;
    /** Q: 0 la quita desde hoy, 1 la vuelve a pedir. */
    private Integer estado;
}
