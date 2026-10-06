// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\RegistrarTareaRutinariaRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;
import java.util.List;

/** Cuerpo del endpoint que crea una tarea rutinaria y la asigna a uno o más cargos. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarTareaRutinariaRequest {

    private String descripcion;
    private long idFrec;
    private Long idArea;
    private Date fechaPartida;
    private Integer idATR;
    private Integer iniFin;
    private List<CargoAsignacionDto> cargos;

    /** Quien registra — se usa tanto para audUsuario como para resolver su cargo/autorización. */
    private long codUsuario;

    /** true (default) = valida que todos los cargos estén en el subárbol del caller; false = modo admin/RRHH sin restricción. */
    private boolean modoJefe = true;

    // nivelMaximoJefe ya NO viaja por Java — el SP lo resuelve internamente
    // desde dbo.tac_configuracion (clave='nivelMaximoJefe'), sin override.
}
