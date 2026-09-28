package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Forma del resultset de <b>p_list_ArticuloProp ACCION 'F'</b>: el comparativo de una
 * propuesta pivoteado por lista de precio. Un solo resultset: es el UNION de tres
 * bloques PIVOT (porcentajes, precio actual y precio propuesto) que se distinguen por
 * la columna {@code det}; la variante "ya aprobada" une solo dos bloques.
 *
 * <p><b>Por eso las doce columnas se llaman valorN y no precioN:</b> segun la fila,
 * valor1..valor12 es un porcentaje o un precio por tonelada. El {@code det} dice cual.
 *
 * <p>El proc devuelve las doce columnas con el nombre del PIVOT, "1".."12", y asi tienen
 * que quedar: las lee por nombre el reporte rptPropuArt.jrxml del JSF, que sigue en
 * produccion. Como un nombre numerico no mapea a ninguna propiedad, el DAO lee la rama
 * como mapa y arma este DTO a mano ({@code ArticuloPropuestoDao.listarComparativoPropuesta}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloPropuestoFDto implements Serializable {

    private Long idPropuesta;

    /** tpr_propuesta.titulo. */
    private String titulo;

    private Integer codigoFamilia;
    private String codArticulo;

    /** Descripcion del articulo; el proc la llama datoArt en las dos variantes. */
    private String datoArt;

    /** Costo por tonelada: subconsulta a tpr_costoSug o tpr_producto.costoTM. */
    private BigDecimal costoTM;

    /** Discriminador de la fila: 'Porcentajes %', 'Precios Ton. Actual', 'Precios Ton. Propuesto' o 'Precios Ton.'. */
    private String det;

    private BigDecimal utm;

    /** Lista de precio 1: la columna "1" del PIVOT. */
    private BigDecimal valor1;
    private BigDecimal valor2;
    private BigDecimal valor3;
    private BigDecimal valor4;
    private BigDecimal valor5;
    private BigDecimal valor6;
    private BigDecimal valor7;
    private BigDecimal valor8;
    private BigDecimal valor9;
    private BigDecimal valor10;
    private BigDecimal valor11;
    private BigDecimal valor12;

    /**
     * El valor de la lista {@code n} (1 a 12). Es lo que usan el DAO para armar la fila
     * y el reporte para recorrer las doce listas sin doce lineas iguales.
     */
    public BigDecimal valorDeLista(int n) {
        switch (n) {
            case 1: return valor1;
            case 2: return valor2;
            case 3: return valor3;
            case 4: return valor4;
            case 5: return valor5;
            case 6: return valor6;
            case 7: return valor7;
            case 8: return valor8;
            case 9: return valor9;
            case 10: return valor10;
            case 11: return valor11;
            case 12: return valor12;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }

    public void ponerValorDeLista(int n, BigDecimal valor) {
        switch (n) {
            case 1: valor1 = valor; break;
            case 2: valor2 = valor; break;
            case 3: valor3 = valor; break;
            case 4: valor4 = valor; break;
            case 5: valor5 = valor; break;
            case 6: valor6 = valor; break;
            case 7: valor7 = valor; break;
            case 8: valor8 = valor; break;
            case 9: valor9 = valor; break;
            case 10: valor10 = valor; break;
            case 11: valor11 = valor; break;
            case 12: valor12 = valor; break;
            default: throw new IllegalArgumentException("Lista fuera de rango: " + n);
        }
    }
}
