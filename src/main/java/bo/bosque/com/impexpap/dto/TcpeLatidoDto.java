package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Cuerpo de {@code /examen-publico/examenes/latido} y {@code /examenes/mis-examenes/latido} (contrato 8.5). El
 * dueño del examen sale del token, nunca de aquí.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeLatidoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Es obligatorio.")
    @Positive(message = "Debe ser un código válido.")
    private Long codExamen;

    /** La pregunta que tiene delante. */
    private Long codPreguntaH;

    /** true si salió de la pestaña o de la aplicación desde el último latido. */
    private Boolean salioPestana;
}
