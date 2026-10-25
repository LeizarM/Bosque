package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * Cuerpo de /tickets/vale/cambiar-lote: un tipo de vale para varios participantes a la vez
 * (p_abm_ttk_ParticipanteTicket M). Las reglas y el mensaje los da el SP.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TtkValeLote implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codEvento;
    private Long codTipoTicket;
    /** null = volver a la cantidad por defecto de cada uno. */
    private Integer cantidad;
    /** codParticipante elegidos; vacía o null = todos los del evento (o los de la categoría). */
    private List<Long> participantes;
    /** E, R o I para limitar a una categoría; null = todas. */
    private String categoria;
}
