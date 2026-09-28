package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_ArticuloProp ACCION 'C'</b>: articulos del catalogo
 * ({@code tpr_articulo}) de una familia, para elegir cuales entran a la propuesta.
 *
 * <p>Ojo: la columna se llama <b>datoArt</b> y no datoArticulo porque sale de
 * tpr_articulo, no de tpr_articuloPropuesto. Se deja tal cual para no cambiarle el
 * nombre a una columna que ya es mapeable.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPropuestoCDto implements Serializable {

    private Integer codigoFamilia;
    private String codArticulo;

    /** tpr_articulo.datoArt, varchar(150). */
    private String datoArt;

    private BigDecimal stock;
    private BigDecimal utm;
}
