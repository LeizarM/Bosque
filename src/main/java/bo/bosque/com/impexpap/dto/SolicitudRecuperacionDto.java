package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pedido de un código de recuperación: solo el usuario. */
@Getter
@Setter
@NoArgsConstructor
public class SolicitudRecuperacionDto {
    private String login;
}
