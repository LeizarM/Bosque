package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_ArticuloProp ACCION 'D'</b>: los articulos afectados
 * por una propuesta con el precio propuesto ya bajado a unidad (precio / utm) y
 * convertido a bolivianos con el tipo de cambio que el proc trae por OPENROWSET.
 *
 * <p><b>La rama tiene dos variantes</b> segun exista o no la propuesta en
 * tpr_articuloPropuesto: la primera lee de tpr_articulo (preliminar) y la segunda de
 * tpr_articuloPropuesto (congelada). Devuelven las mismas 19 columnas en el mismo orden,
 * pero en el legacy cinco de ellas se llamaban distinto en cada variante
 * (costo/costoTM, datoArt/datoArticulo, 'Lista de Precio'/listaPrecio) y ocho tenian
 * PARENTESIS en el alias, que BeanPropertyRowMapper no puede mapear a una propiedad
 * Java: el objeto se llenaba a medias y sin error. El script tpr_ArticuloPropuesto.sql
 * unifica los nombres y saca los parentesis conservando cantidad y orden de columnas.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPropuestoDDto implements Serializable {

    private Integer codigoFamilia;

    /**
     * Variante preliminar: subconsulta a tpr_costoSug de esta propuesta (antes se
     * llamaba "costo"). Variante congelada: tpr_producto.costoTM.
     */
    private BigDecimal costoTM;

    private String proveedorExtSap;

    /** tpr_grupoFamiliaSap.grpFam. */
    private String grpFam;

    private String codArticulo;

    /** Descripcion del articulo (antes "datoArt" en la variante preliminar). */
    private String datoArticulo;

    /** tb_sucursal.nombre. */
    private String nombre;

    /** vpp = numero de lista de precios visible al usuario (antes 'Lista de Precio'). */
    private Integer listaPrecio;

    /** listNum remapeado por el CASE del proc (numeracion PAPIRUS). */
    private Integer listNum;

    /** Mismo CASE que listNum; en el legacy es identico valor (numeracion IMPEXPAP). */
    private Integer listNumIpx;

    private BigDecimal utm;

    /** tpr_precioPropuesta.precioPropuesto, precio por tonelada. */
    private BigDecimal precioPropuesto;

    /** precioPropuesto / utm. Antes 'Precio Unitario USD(IPX)'. */
    private BigDecimal precioUnitUsdIpx;

    /**
     * Literal 0.0 sin alias en el legacy. Es el espacio del incremento del 15% que
     * quedo comentado en el proc; se le da nombre para que no sea una columna anonima.
     */
    private BigDecimal precioUnitUsdIpxInc;

    /** (precioPropuesto / utm) * tipoCambio. */
    private BigDecimal precioUnitBsIpx;

    /**
     * ((precioPropuesto / utm) - 7%) * tipoCambio. El alias del legacy decia USD/EXE
     * pero la expresion multiplica por el tipo de cambio: el valor esta en bolivianos.
     * Se conserva el calculo tal cual; solo se corrigio el alias para que mapee.
     */
    private BigDecimal precioUnitUsdProdpap;

    /** ((precioPropuesto / utm) * tipoCambio) - 7%. */
    private BigDecimal precioUnitBsProdpap;

    /** Literal 'BS' en el proc, pese al nombre. */
    private String monedaUsd;

    /** Literal 'BS'. */
    private String monedaBs;
}
