package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Cuerpo de {@code /examenes/banco/eliminar} ({@code codPregunta}) y {@code /activar} ({@code codPregunta, activo}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoAccionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Es obligatorio.")
    @Positive(message = "Debe ser un código válido.")
    private Long codPregunta;

    private Boolean activo;
}
