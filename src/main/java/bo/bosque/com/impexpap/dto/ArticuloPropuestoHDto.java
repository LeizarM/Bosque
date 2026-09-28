package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_ArticuloProp ACCION 'H'</b>: el reporte para
 * actualizar precios por articulo, pivoteado por lista de precio. Las dos variantes
 * (preliminar desde tpr_precio, o historica desde tpr_bitArticulo) devuelven la misma
 * forma; solo cambia de donde sale el precio.
 *
 * <p>Aca las doce columnas SI son siempre precios por tonelada, por eso precioN.
 *
 * <p>Igual que en la rama F, las columnas llegan con el nombre del PIVOT, "1".."12",
 * porque las lee por nombre RptArtPropuPorArticulo.jrxml del JSF. El DAO lee la rama
 * como mapa y arma este DTO a mano.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPropuestoHDto implements Serializable {

    private Long idPropuesta;

    /** tpr_propuesta.obs. */
    private String obs;

    /**
     * tpr_propuesta.titulo. En la variante preliminar el proc le concatena
     * " (Nuevos Articulos)".
     */
    private String titulo;

    private Integer codigoFamilia;

    /** Costo por tonelada. */
    private BigDecimal costoTM;

    private String codArticulo;
    private String datoArticulo;
    private BigDecimal utm;

    /** Precio por tonelada de la lista 1: la columna "1" del PIVOT. */
    private BigDecimal precio1;
    private BigDecimal precio2;
    private BigDecimal precio3;
    private BigDecimal precio4;
    private BigDecimal precio5;
    private BigDecimal precio6;
    private BigDecimal precio7;
    private BigDecimal precio8;
    private BigDecimal precio9;
    private BigDecimal precio10;
    private BigDecimal precio11;
    private BigDecimal precio12;

    /**
     * El precio por tonelada de la lista {@code n} (1 a 12). Es lo que usan el DAO para armar la fila
     * y el reporte para recorrer las doce listas sin doce lineas iguales.
     */
    public BigDecimal precioDeLista(int n) {
        switch (n) {
            case 1: return precio1;
            case 2: return precio2;
            case 3: return precio3;
            case 4: return precio4;
            case 5: return precio5;
            case 6: return precio6;
            case 7: return precio7;
            case 8: return precio8;
            case 9: return precio9;
            case 10: return precio10;
            case 11: return precio11;
            case 12: return precio12;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }

    public void ponerPrecioDeLista(int n, BigDecimal valor) {
        switch (n) {
            case 1: precio1 = valor; break;
            case 2: precio2 = valor; break;
            case 3: precio3 = valor; break;
            case 4: precio4 = valor; break;
            case 5: precio5 = valor; break;
            case 6: precio6 = valor; break;
            case 7: precio7 = valor; break;
            case 8: precio8 = valor; break;
            case 9: precio9 = valor; break;
            case 10: precio10 = valor; break;
            case 11: precio11 = valor; break;
            case 12: precio12 = valor; break;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }
}
