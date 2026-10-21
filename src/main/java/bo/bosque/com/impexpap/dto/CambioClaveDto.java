package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cambio de la propia contraseña (pantalla tbUsuario/cpassword): la actual, la nueva y su repetición.
 * El usuario sale del token, nunca del cuerpo.
 */
@Getter
@Setter
@NoArgsConstructor
public class CambioClaveDto implements Serializable {

    private String actual;
    private String nueva;
    private String confirmacion;
}
