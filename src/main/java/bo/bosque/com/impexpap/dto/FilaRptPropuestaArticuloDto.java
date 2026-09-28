package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Un articulo del PDF de una propuesta por articulo ({@code RptArtPropuPorArticulo.jrxml}),
 * con el precio por tonelada vigente de su familia en cada lista.
 *
 * <p>Sale de la rama H de {@code p_list_ArticuloProp}, mas el numero de la ultima
 * propuesta aprobada de la familia ({@code tpr_producto.idPropuestaAprobada}): es la que
 * fijo los precios que el articulo nuevo va a tomar. La familia y esa propuesta se dibujan
 * como celdas combinadas sobre los articulos de la familia ({@link #primeroDeFamilia} /
 * {@link #ultimoDeFamilia}); el costo va en cada articulo.
 *
 * <p>Lo arma {@code ReportesPreciosService.filasPorArticulo}. Los getters son los
 * {@code <field>} del jrxml.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class FilaRptPropuestaArticuloDto implements Serializable {

    private Integer codigoFamilia;

    /** La ultima propuesta aprobada de la familia; null si nunca tuvo una. */
    private Long ultimaPropuesta;

    /** Costo por tonelada actual de la familia. */
    private BigDecimal costoTM;

    private String codArticulo;
    private String datoArticulo;
    private BigDecimal utm;

    private boolean primeroDeFamilia;
    private boolean ultimoDeFamilia;

    /** Precio por tonelada actual de la lista 1..12. */
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

    public BigDecimal precio(int n) {
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

    public void ponerPrecio(int n, BigDecimal v) {
        switch (n) {
            case 1: precio1 = v; break;
            case 2: precio2 = v; break;
            case 3: precio3 = v; break;
            case 4: precio4 = v; break;
            case 5: precio5 = v; break;
            case 6: precio6 = v; break;
            case 7: precio7 = v; break;
            case 8: precio8 = v; break;
            case 9: precio9 = v; break;
            case 10: precio10 = v; break;
            case 11: precio11 = v; break;
            case 12: precio12 = v; break;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }
}
