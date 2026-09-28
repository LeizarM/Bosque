package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_producto @ACCION='A'</b>: cada precio vigente
 * de cada familia activa (tpr_producto cruzado con tpr_precio), con las
 * descripciones de proveedor, grupo de familia, color y tipo.
 *
 * <p>Requiere el ALTER de {@code tpr_Producto.sql}: en el legacy la columna 12 del
 * SELECT era el literal 0 SIN ALIAS, por lo que llegaba con nombre vacio y nunca
 * podia mapearse. El ALTER le pone el alias {@code valorCero} sin mover su posicion,
 * para no romper al JSF que lee por indice.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductoPrecioDto {

    private Long idPrecio;

    /** ROW_NUMBER() por familia: numero de orden del precio dentro de la familia. */
    private Long cantPrecio;

    private Integer codigoFamilia;

    /** float(53) en la base; BigDecimal para no perder centavos. */
    private BigDecimal precio;

    /** Id crudo de la presentacion, no el texto. */
    private Long idPresentacion;

    private String proveedorExtSap;

    private String grpFam;

    private String formato;

    private String gramaje;

    private String color;

    private String tipo;

    /**
     * Literal 0 que el SP legacy devuelve sin alias al final del SELECT. Se mantiene
     * por compatibilidad de posiciones con el JSF; su proposito original se desconoce.
     */
    private Integer valorCero;
}
