package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo de {@code POST /cheque/verificacion/registrar}: alta ({@code codvd} 0 o ausente) o edicion.
 *
 * <p>No lleva {@code estado} ni usuario: el estado lo decide el servidor (alta = {@code Y}; la edicion conserva el que
 * tenia, y anular es otro endpoint) y el usuario sale del token. En la edicion el {@code codCheque} tampoco se cambia:
 * se ignora el que llegue.
 */
@Getter
@Setter
@NoArgsConstructor
public class VerificacionRegistroDto implements Serializable {

    private Integer codvd;
    private Long codCheque;
    private Long codBanco;

    @FechaDiaJson
    private LocalDate fechaBanco;

    private String observacion;

    public boolean esAlta() {
        return codvd == null || codvd <= 0;
    }
}
