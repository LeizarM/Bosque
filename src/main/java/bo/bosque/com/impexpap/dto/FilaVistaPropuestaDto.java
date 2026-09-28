package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Un articulo de la vista preliminar, con sus doce listas en el orden del VPP (indice 0 =
 * lista 1). Es una fila del PDF sin el pivote: {@link FilaRptPropuestaFamiliaDto} en las
 * propuestas por familia, {@link FilaRptPropuestaArticuloDto} en las de articulo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FilaVistaPropuestaDto implements Serializable {

    private Integer codigoFamilia;

    /** Grupo y proveedor de la familia, de la rama D o G; vacio si no los trae. */
    private String descripcionFamilia;

    /** Solo en las propuestas por articulo: la ultima aprobada de la familia; null si no hubo. */
    private Long ultimaPropuesta;

    private String codArticulo;
    private String descripcion;
    private BigDecimal utm;

    /** Costo por tonelada en USD, el de la columna COSTO del PDF. */
    private BigDecimal costoTM;

    /** Si trae la fila "Precio Ton. Actual" (y los colores del propuesto). */
    private boolean conFilaActual;

    /** Porcentaje de cada lista. Vacia en las propuestas por articulo. */
    private List<BigDecimal> porcentajes = new ArrayList<>();

    /** Precio por tonelada vigente antes de la propuesta. Vacia si no hay comparacion. */
    private List<BigDecimal> actuales = new ArrayList<>();

    /**
     * Precio por tonelada propuesto; en las propuestas por articulo, el vigente de la
     * familia, que es el que toma el articulo.
     */
    private List<BigDecimal> propuestos = new ArrayList<>();

    /** Por lista: 1 si el propuesto sube, -1 si baja, 0 igual, null sin comparacion. */
    private List<Integer> cambios = new ArrayList<>();

    /** Precio por unidad en USD (Impexpap): precio por tonelada / UTM. */
    private List<BigDecimal> unidadUsd = new ArrayList<>();

    /**
     * Precio por unidad en Bs (Impexpap): por tonelada / UTM x tipo de cambio. Es la
     * columna "Precio Unit. IPX (BS)" del Excel de la generacion.
     */
    private List<BigDecimal> unidadBs = new ArrayList<>();

    /**
     * Precio por unidad en Bs de Productiva: el de Impexpap menos 7 %. Es la columna
     * "Precio Unit. PRODUCTIVA (BS)" del Excel.
     */
    private List<BigDecimal> unidadBsProductiva = new ArrayList<>();
}
