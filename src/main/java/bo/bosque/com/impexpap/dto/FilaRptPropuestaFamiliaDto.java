package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Un articulo del PDF de una propuesta por familia ({@code rptPropuArt.jrxml}), con sus
 * tres filas de doce valores.
 *
 * <p>La rama F de {@code p_list_ArticuloProp} devuelve una fila por articulo y por
 * concepto ({@code det}): porcentaje, precio actual y precio propuesto. El reporte del
 * JSF imprimia esas filas tal cual y dejaba en blanco el codigo y la descripcion en la
 * segunda y la tercera, asi que no se veia que las tres eran del mismo articulo. Aca se
 * juntan: una fila por articulo, y el jrxml dibuja el codigo, la descripcion y el UTM
 * como una celda combinada sobre sus tres filas de valores. La familia se combina sobre
 * todos sus articulos con {@link #primeroDeFamilia} y {@link #ultimoDeFamilia}; el costo
 * va en cada articulo.
 *
 * <p>Lo arma {@code ReportesPreciosService.filasCombinadas}. Los getters son los
 * {@code <field>} del jrxml.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class FilaRptPropuestaFamiliaDto implements Serializable {

    private Integer codigoFamilia;
    private String codArticulo;
    private String datoArt;
    private BigDecimal utm;

    /** Costo por tonelada de la familia en la propuesta. */
    private BigDecimal costoTM;

    /** El primer articulo de su familia: abre la celda combinada de familia. */
    private boolean primeroDeFamilia;

    /** El ultimo articulo de su familia: la cierra. */
    private boolean ultimoDeFamilia;

    /**
     * Propuesta todavia no aprobada: tres filas (porcentaje, actual, propuesto). Una
     * aprobada trae dos -porcentaje y "Precios Ton."- y el precio va en pro1..pro12.
     */
    private boolean conFilaActual;

    /** Los rotulos de cada fila, tal como los manda el proc en la columna det. */
    private String etiqueta1;
    private String etiqueta2;
    private String etiqueta3;

    /** Porcentaje de la lista 1..12 ("Porcentajes %", "Precios Ton. Actual", "Precios Ton. Propuesto"). */
    private BigDecimal por1;
    private BigDecimal por2;
    private BigDecimal por3;
    private BigDecimal por4;
    private BigDecimal por5;
    private BigDecimal por6;
    private BigDecimal por7;
    private BigDecimal por8;
    private BigDecimal por9;
    private BigDecimal por10;
    private BigDecimal por11;
    private BigDecimal por12;

    /** Precio por tonelada actual de la lista 1..12 ("Porcentajes %", "Precios Ton. Actual", "Precios Ton. Propuesto"). */
    private BigDecimal act1;
    private BigDecimal act2;
    private BigDecimal act3;
    private BigDecimal act4;
    private BigDecimal act5;
    private BigDecimal act6;
    private BigDecimal act7;
    private BigDecimal act8;
    private BigDecimal act9;
    private BigDecimal act10;
    private BigDecimal act11;
    private BigDecimal act12;

    /** Precio por tonelada propuesto de la lista 1..12 ("Porcentajes %", "Precios Ton. Actual", "Precios Ton. Propuesto"). */
    private BigDecimal pro1;
    private BigDecimal pro2;
    private BigDecimal pro3;
    private BigDecimal pro4;
    private BigDecimal pro5;
    private BigDecimal pro6;
    private BigDecimal pro7;
    private BigDecimal pro8;
    private BigDecimal pro9;
    private BigDecimal pro10;
    private BigDecimal pro11;
    private BigDecimal pro12;

    /** +1 si el propuesto de la lista sube respecto del actual, -1 si baja, 0 igual; null sin los dos. */
    private Integer cambio1;
    private Integer cambio2;
    private Integer cambio3;
    private Integer cambio4;
    private Integer cambio5;
    private Integer cambio6;
    private Integer cambio7;
    private Integer cambio8;
    private Integer cambio9;
    private Integer cambio10;
    private Integer cambio11;
    private Integer cambio12;

    public BigDecimal porcentaje(int n) {
        switch (n) {
            case 1: return por1;
            case 2: return por2;
            case 3: return por3;
            case 4: return por4;
            case 5: return por5;
            case 6: return por6;
            case 7: return por7;
            case 8: return por8;
            case 9: return por9;
            case 10: return por10;
            case 11: return por11;
            case 12: return por12;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }

    public void ponerPorcentaje(int n, BigDecimal v) {
        switch (n) {
            case 1: por1 = v; break;
            case 2: por2 = v; break;
            case 3: por3 = v; break;
            case 4: por4 = v; break;
            case 5: por5 = v; break;
            case 6: por6 = v; break;
            case 7: por7 = v; break;
            case 8: por8 = v; break;
            case 9: por9 = v; break;
            case 10: por10 = v; break;
            case 11: por11 = v; break;
            case 12: por12 = v; break;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }

    public BigDecimal actual(int n) {
        switch (n) {
            case 1: return act1;
            case 2: return act2;
            case 3: return act3;
            case 4: return act4;
            case 5: return act5;
            case 6: return act6;
            case 7: return act7;
            case 8: return act8;
            case 9: return act9;
            case 10: return act10;
            case 11: return act11;
            case 12: return act12;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }

    public void ponerActual(int n, BigDecimal v) {
        switch (n) {
            case 1: act1 = v; break;
            case 2: act2 = v; break;
            case 3: act3 = v; break;
            case 4: act4 = v; break;
            case 5: act5 = v; break;
            case 6: act6 = v; break;
            case 7: act7 = v; break;
            case 8: act8 = v; break;
            case 9: act9 = v; break;
            case 10: act10 = v; break;
            case 11: act11 = v; break;
            case 12: act12 = v; break;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }

    public BigDecimal propuesto(int n) {
        switch (n) {
            case 1: return pro1;
            case 2: return pro2;
            case 3: return pro3;
            case 4: return pro4;
            case 5: return pro5;
            case 6: return pro6;
            case 7: return pro7;
            case 8: return pro8;
            case 9: return pro9;
            case 10: return pro10;
            case 11: return pro11;
            case 12: return pro12;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }

    public void ponerPropuesto(int n, BigDecimal v) {
        switch (n) {
            case 1: pro1 = v; break;
            case 2: pro2 = v; break;
            case 3: pro3 = v; break;
            case 4: pro4 = v; break;
            case 5: pro5 = v; break;
            case 6: pro6 = v; break;
            case 7: pro7 = v; break;
            case 8: pro8 = v; break;
            case 9: pro9 = v; break;
            case 10: pro10 = v; break;
            case 11: pro11 = v; break;
            case 12: pro12 = v; break;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }

    public Integer cambio(int n) {
        switch (n) {
            case 1: return cambio1;
            case 2: return cambio2;
            case 3: return cambio3;
            case 4: return cambio4;
            case 5: return cambio5;
            case 6: return cambio6;
            case 7: return cambio7;
            case 8: return cambio8;
            case 9: return cambio9;
            case 10: return cambio10;
            case 11: return cambio11;
            case 12: return cambio12;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }

    public void ponerCambio(int n, Integer v) {
        switch (n) {
            case 1: cambio1 = v; break;
            case 2: cambio2 = v; break;
            case 3: cambio3 = v; break;
            case 4: cambio4 = v; break;
            case 5: cambio5 = v; break;
            case 6: cambio6 = v; break;
            case 7: cambio7 = v; break;
            case 8: cambio8 = v; break;
            case 9: cambio9 = v; break;
            case 10: cambio10 = v; break;
            case 11: cambio11 = v; break;
            case 12: cambio12 = v; break;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }
}
