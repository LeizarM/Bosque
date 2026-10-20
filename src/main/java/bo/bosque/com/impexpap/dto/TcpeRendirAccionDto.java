package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Cuerpo de los endpoints de rendición ({@code estado}, {@code preguntas}, {@code opciones}, {@code iniciar}, {@code responder}, {@code finalizar}, {@code revision}). El dueño del examen sale del token, nunca de aquí.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRendirAccionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Es obligatorio.")
    @Positive(message = "Debe ser un código válido.")
    private Long codExamen;

    /** Solo para responder. */
    private Long codPreguntaH;

    /** Cerrada: códigos {@code codRsptaOpH} marcados; lista vacía = sin responder. */
    private List<@NotNull(message = "Hay una opción vacía en la lista.") Long> opciones;

    /** Abierta: el texto de la respuesta. */
    private String respuestaU;
}
