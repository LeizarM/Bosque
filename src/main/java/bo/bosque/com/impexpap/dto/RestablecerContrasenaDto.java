package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Código recibido por correo más la contraseña nueva. */
@Getter
@Setter
@NoArgsConstructor
public class RestablecerContrasenaDto {
    private String login;
    private String codigo;
    private String nuevaContrasena;
}
