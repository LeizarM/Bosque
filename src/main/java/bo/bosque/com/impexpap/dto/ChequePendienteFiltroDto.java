package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cuerpo de {@code POST /cheque/verificacion/pendientes}: el modal "Cheques Pendientes Sin Regularizar".
 *
 * <ul>
 *   <li>{@code estado}: {@code PEN} o {@code CER}; ausente o vacio = todos (el "Todos" del JSF; la pantalla manda
 *       PEN por defecto, como el legacy).</li>
 *   <li>{@code soloCobranzaHoy}: true o ausente = solo los que tienen la fecha de cobranza de hoy (el "2" del JSF,
 *       su valor por defecto); false = todos los anteriores y los de hoy (el "1").</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class ChequePendienteFiltroDto implements Serializable {

    private String estado;
    private Boolean soloCobranzaHoy;

    /** Desde 1. */
    private Integer pagina;

    /** 20 por defecto, 200 como maximo. */
    private Integer tamanio;
}
