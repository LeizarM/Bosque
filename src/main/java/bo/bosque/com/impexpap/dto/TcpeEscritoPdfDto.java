package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Cuerpo de {@code /examenes/lotes/examen-escrito-pdf} y {@code /solucionario-pdf}: el lote y, si se quiere uno
 * solo, el examen.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeEscritoPdfDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Es obligatorio.")
    @Positive(message = "Debe ser un código válido.")
    private Long codRolExamen;

    /** Nulo = todos los exámenes del lote. */
    private Long codExamen;
}
