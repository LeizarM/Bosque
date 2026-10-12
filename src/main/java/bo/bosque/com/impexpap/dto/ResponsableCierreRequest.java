// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\ResponsableCierreRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Alta, cambio o baja de un responsable del cierre. Las reglas (sección
 * válida, cargo o persona, sucursal solo en arqueos y caja fuerte, sin
 * duplicados) las revisa {@code p_abm_tac_ResponsableCierre}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ResponsableCierreRequest {
    private Long idRC;
    private Long idTarRuti;
    private String seccion;
    private Long codSucursal;
    private Long codCargoSucursal;
    private Long codEmpleado;
    private Integer puedeAutorrevisar;
    private Integer estado;
}
