package bo.bosque.com.impexpap.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Cuerpo de {@code /examenes/banco/guardar}: la pregunta, su respuesta y sus opciones en una sola operación.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeBancoGuardarDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Nulo o 0 = alta. */
    private Long codPregunta;

    @NotNull(message = "Es obligatorio.")
    @Positive(message = "Debe ser un código válido.")
    private Long codClasificacion;

    @NotBlank(message = "Es obligatorio.")
    private String pregunta;

    private Integer prioridad;

    private Boolean activo;

    /** true = con opciones; false = abierta (la califica el evaluador). */
    @NotNull(message = "Es obligatorio.")
    private Boolean esCerrada;

    /** Respuesta modelo de una abierta. */
    private String respuesta;

    /** Cuántas puede tener lo decide el SP ({@code maxOpcionesPorPregunta} de {@code f_tcpe_config}). */
    @Valid
    private List<@NotNull(message = "Hay una opción vacía en la lista.") TcpeBancoOpcionDto> opciones;
}
