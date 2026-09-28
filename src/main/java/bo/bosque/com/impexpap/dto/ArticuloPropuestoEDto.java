package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_ArticuloProp ACCION 'E'</b>: los articulos de VARIAS
 * familias a la vez (la lista de familias llega en {@code @codCad} como "12,13,14,").
 *
 * <p>Es la forma de la ACCION 'C' mas {@code filaCod}, el correlativo
 * {@code ROW_NUMBER() OVER(PARTITION BY codigoFamilia)} que usa la pantalla para
 * numerar dentro de cada familia.
 *
 * <p>La rama arma la consulta con {@code sp_executesql} concatenando {@code @codCad}:
 * el DAO valida que sean solo digitos y comas antes de mandarla.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPropuestoEDto implements Serializable {

    /** ROW_NUMBER() por familia; empieza en 1 en cada codigoFamilia. */
    private Long filaCod;

    private String codArticulo;
    private Integer codigoFamilia;

    /** tpr_articulo.datoArt, varchar(150). */
    private String datoArt;

    private BigDecimal stock;
    private BigDecimal utm;
}
