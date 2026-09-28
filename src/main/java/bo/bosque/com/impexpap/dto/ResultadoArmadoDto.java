package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Lo que hizo una escritura del armado que no devuelve grilla: fletes y
 * articulos. Los contadores que no aplican a la operacion quedan en 0.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoArmadoDto implements Serializable {

    /** La propuesta afectada; en un alta, la recien creada. */
    private Long idPropuesta;

    /** Articulos agregados o quitados. */
    private int articulos;

    /** Articulos que ya estaban en la propuesta y no se volvieron a agregar. */
    private int omitidos;

    /** Familias cuyos precios se recalcularon por un cambio de flete. */
    private int familiasRecalculadas;

    /** Listas de precios escritas por el recalculo. */
    private int lineasEscritas;
}
