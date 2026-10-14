package bo.bosque.com.impexpap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Respuesta al pedir un código: a dónde se envió (enmascarado), cuántos minutos vale y cuándo se puede pedir otro. */
@Getter
@AllArgsConstructor
public class RecuperacionEnviadaDto {
    private final String correo;
    private final int minutosVigencia;
    private final int segundosParaReenviar;
}
