package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Una familia de la carga en lote del asistente: su codigo y el costo propuesto.
 *
 * <p>No lleva porcentajes: en lote cada familia se calcula con los de
 * {@code tpr_porcentaje}, lista por lista. Para cambiar el porcentaje de una familia se la
 * abre en el editor, que es donde se ve contra que precios se esta comparando.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FamiliaLoteDto implements Serializable {

    /** Obligatorio. */
    private Integer codigoFamilia;

    /** Costo propuesto en USD por tonelada. Obligatorio y mayor a cero. */
    private BigDecimal costo;
}
