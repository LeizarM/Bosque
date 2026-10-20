package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Cuerpo de {@code /examen-publico/auth/login}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeLoginPostulanteDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Es obligatorio.")
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String login;

    @NotBlank(message = "Es obligatorio.")
    @Size(max = 100, message = "Máximo 100 caracteres.")
    @ToString.Exclude
    private String password2;
}
