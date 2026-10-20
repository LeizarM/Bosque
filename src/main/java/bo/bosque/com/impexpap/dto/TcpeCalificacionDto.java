package bo.bosque.com.impexpap.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Cuerpo de {@code /examenes/calificacion/marcar} ({@code codExamen, codRespuestaH, esCorrecta}), {@code /cerrar} ({@code codExamen}) y {@code /nota} ({@code codExamen, resultado}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeCalificacionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Es obligatorio.")
    @Positive(message = "Debe ser un código válido.")
    private Long codExamen;

    private Long codRespuestaH;

    private Boolean esCorrecta;

    /** Nota manual; el rango lo valida {@code p_abm_tcpe_Calificacion 'N'}. */
    private Double resultado;
}
