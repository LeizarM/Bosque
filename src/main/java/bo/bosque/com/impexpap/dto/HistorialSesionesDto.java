package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/** Filtro del historial de sesiones: días completos, de un usuario o de todos (codUsuario vacío). */
@Getter
@Setter
@NoArgsConstructor
public class HistorialSesionesDto {
    private Date desde;
    private Date hasta;
    private Long codUsuario;
}
