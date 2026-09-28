package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_ArticuloProp ACCION 'G'</b>: la pantalla de articulos
 * de una propuesta cuando se crean items nuevos o entra mercaderia. Misma grilla que la
 * ACCION 'D' pero con el precio VIGENTE (tpr_precio.precio) en vez del propuesto.
 *
 * <p><b>Dos variantes</b> segun exista la propuesta en tpr_bitArticulo (la bitacora de
 * articulos ya repreciados). Igual que en la rama D, el legacy les ponia nombres
 * distintos a las mismas posiciones y dejaba tres columnas sin alias (dos NULL y un
 * 0.0); el script tpr_ArticuloPropuesto.sql las nombra y unifica.
 *
 * <p><b>Divergencia semantica heredada, no corregida:</b> en la variante que lee de
 * tpr_articuloPropuesto la columna 7 es el nombre de la SUCURSAL y en la que lee de
 * tpr_bitArticulo es el nombre de la LISTA DE PRECIO. Se unifico el alias a
 * {@code nombre} para que el POJO mapee en las dos, pero el contenido no significa lo
 * mismo. Reportado como bug del legacy.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPropuestoGDto implements Serializable {

    private Integer codigoFamilia;

    /** tpr_producto.costoTM (o tpr_bitArticulo.costoTM en la variante bitacora). */
    private BigDecimal costoTM;

    /**
     * Siempre NULL: en el legacy era una columna {@code null} sin alias, puesta para
     * que la forma coincida con la de la ACCION 'D'. Se le dio nombre, no valor.
     */
    private String proveedorExtSap;

    /** Siempre NULL, mismo motivo que proveedorExtSap. */
    private String grpFam;

    private String codArticulo;
    private String datoArticulo;

    /** Variante 1: nombre de la sucursal. Variante 2: nombre de la lista de precio. */
    private String nombre;

    /** vpp = numero de lista de precios visible al usuario. */
    private Integer listaPrecio;

    private Integer listNum;
    private Integer listNumIpx;

    private BigDecimal utm;

    /** tpr_precio.precio vigente, por tonelada. */
    private BigDecimal precio;

    /** precio / utm. */
    private BigDecimal precioUnitUsdIpx;

    /** Literal 0.0, incremento comentado en el proc. */
    private BigDecimal precioUnitUsdIpxInc;

    /** (precio / utm) * tipoCambio. */
    private BigDecimal precioUnitBsIpx;

    /**
     * Ojo: la variante que lee de tpr_articuloPropuesto multiplica por el tipo de
     * cambio y la que lee de tpr_bitArticulo NO. O sea que la misma columna sale en
     * bolivianos o en dolares segun por donde entre. Es un bug del legacy: se reporta
     * y se conserva el calculo tal cual.
     */
    private BigDecimal precioUnitUsdProdpap;

    /** ((precio / utm) * tipoCambio) - 7%. */
    private BigDecimal precioUnitBsProdpap;

    /** Literal 'BS', pese al nombre. */
    private String monedaUsd;

    /** Literal 'BS'. */
    private String monedaBs;
}
