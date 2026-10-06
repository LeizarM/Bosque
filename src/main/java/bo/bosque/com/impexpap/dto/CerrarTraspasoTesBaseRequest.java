// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\CerrarTraspasoTesBaseRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Cuerpo de /tareas-rutinarias/traspaso-efectivo-tesbase/{cerrar, sin-pendientes,
 * dia-revisado, del-dia}.
 *
 * <p>Sin audUsuario: sale del token.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CerrarTraspasoTesBaseRequest {

    /** La transferencia a cerrar. Solo para "cerrar". */
    private Long codTes;

    /** La ocurrencia de la tarea desde la que se trabaja. */
    private Long idBitTarRuti;

    /** El día a consultar. Solo para "del-dia" (archivo SQL 58). */
    private Date fecha;
}
