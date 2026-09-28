package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma de la rama <b>S</b> de {@code p_list_producto}: el tipo de cambio vigente
 * leido del sistema IMPEXPAP/SOFI por OPENROWSET.
 *
 * <p>Requiere el ALTER de {@code tpr_Producto.sql}: el legacy hacia {@code Select @tc}
 * sin alias, o sea una columna con nombre vacio que jamas mapeaba a una propiedad
 * Java (el objeto llegaba entero en null y sin excepcion). El ALTER lo deja como
 * {@code SELECT @tc AS tc}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductoTipoCambioDto {

    /** float(53) en el origen; BigDecimal para no perder decimales del tipo de cambio. */
    private BigDecimal tc;
}
