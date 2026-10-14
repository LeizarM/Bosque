package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** La sesión que un administrador cierra desde la pantalla Sesiones. */
@Getter
@Setter
@NoArgsConstructor
public class CerrarSesionDto {
    private Long idSesion;
}
