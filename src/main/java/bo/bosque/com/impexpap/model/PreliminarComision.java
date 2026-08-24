package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

/**
 * Fila de la vista preliminar de comisiones (p_list_paraPagar, ramas F, I, J, K).
 * <p>
 * Es la union de las cuatro formas que devuelve el SP. Cada rama trae un
 * subconjunto distinto y los alias no coinciden entre si, porque en un UNION ALL
 * los nombres los fija la primera consulta:
 *
 * <pre>
 *   F, I : ord, idVendedor, mes, anio, grupo, nombreVen, comision,
 *          montoPagadoBS, bsAPagar, usd
 *   J    : ord, idVendedor, mes, anio, grupo, nombreVen, comision,
 *          ignoraComision, montoCerradoBS, ventaTotalMesUSD, bsAPagar, usdAPagar
 *   K    : ord, idv, mes, anio, tipo, nombreVen, comision, ignoraComision,
 *          montoCerradoBS, montoTotalBS, bsAPagar, usdAPagar
 * </pre>
 *
 * Los campos que la rama no devuelve quedan en null; BeanPropertyRowMapper solo
 * completa las columnas presentes. Los getters normalizados del final resuelven
 * las diferencias de alias para que el cliente no tenga que conocerlas.
 * <p>
 * <b>ord</b> no es un dato del negocio sino de presentacion: el SP lo usa para
 * ordenar y para distinguir detalle de totales.
 * 1 = detalle, 2 = subtotal por vendedor o empresa, 3 = grupos que no son de
 * venta, 4 = separador o totales segun la rama.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreliminarComision implements Serializable {

    private Integer ord;

    // idVendedor en F, I y J; idv en K.
    private Long idVendedor;
    private Long idv;

    private Integer mes;
    private Integer anio;

    // grupo en F, I y J; tipo en K (Contado, Credito, Comision Anterior).
    private String grupo;
    private String tipo;

    private String nombreVen;

    /** Factor ya dividido entre 100: 0.006 equivale a 0,6%. */
    private BigDecimal comision;

    private Integer ignoraComision;

    // Monto base. El alias cambia por rama.
    private BigDecimal montoPagadoBS;    // F, I
    private BigDecimal montoCerradoBS;   // J, K
    private BigDecimal montoTotalBS;     // solo K

    private BigDecimal ventaTotalMesUSD; // solo J

    private BigDecimal bsAPagar;

    // usd en F e I; usdAPagar en J y K.
    private BigDecimal usd;
    private BigDecimal usdAPagar;

    // ---------- Normalizadores ----------

    /** Id del vendedor, venga como idVendedor o como idv. */
    public Long getIdVendedorNormalizado() {
        return idVendedor != null ? idVendedor : idv;
    }

    /** Etiqueta de la fila: grupo en la mayoria de las ramas, tipo en la K. */
    public String getEtiqueta() {
        return grupo != null ? grupo : tipo;
    }

    /** Monto base sobre el que se calcula, sea cual sea el alias de la rama. */
    public BigDecimal getMontoBase() {
        if (montoPagadoBS != null)  return montoPagadoBS;
        if (montoCerradoBS != null) return montoCerradoBS;
        return montoTotalBS;
    }

    /** Importe en dolares, venga como usd o como usdAPagar. */
    public BigDecimal getUsdNormalizado() {
        return usd != null ? usd : usdAPagar;
    }

    /** true cuando la fila es un total y no un detalle. */
    public boolean esTotal() {
        return ord != null && ord >= 2;
    }
}
