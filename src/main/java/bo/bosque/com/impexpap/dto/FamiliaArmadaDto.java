package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Una familia que ya esta en la propuesta.
 *
 * <p>La lista sale de {@code tpr_costoSug}: el legacy graba un costo sugerido por
 * cada familia que se carga, y es esa fila la que despues actualiza
 * {@code tpr_producto.costoTM} al aprobar. Por eso es la fuente de verdad de
 * "que familias toca la propuesta", y no los articulos, que se derivan.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FamiliaArmadaDto implements Serializable {

    private Integer codigoFamilia;

    /** Costo propuesto en USD por tonelada. */
    private BigDecimal costoSug;

    /** Cuantas listas de precios de la familia tienen precio propuesto. */
    private int lineas;
}
