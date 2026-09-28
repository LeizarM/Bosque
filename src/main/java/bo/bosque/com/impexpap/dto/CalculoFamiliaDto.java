package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de {@code calcularFamilia} y {@code guardarFamilia}: la grilla de la
 * familia con el precio que resulta del costo propuesto, y lo que se escribio o
 * se escribiria.
 *
 * <p>Las dos operaciones devuelven lo mismo a proposito: la pantalla muestra la
 * vista previa, y al guardar recibe exactamente la grilla que se grabo, calculada
 * por el servidor. El cliente nunca calcula un precio que despues se persista.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalculoFamiliaDto implements Serializable {

    /** Propuesta a la que pertenece la grilla. Nulo en la vista previa de un alta. */
    private Long idPropuesta;

    private Integer codigoFamilia;

    /** Descripcion de la familia resuelta por el backend, para el encabezado. */
    private String grupoFamilia;
    private String proveedor;
    private String presentacion;
    private String tipo;
    private String rangoGramaje;
    private String color;

    /** Costo con el que se calculo. Nulo si no hubo con que calcular. */
    private BigDecimal costo;

    /** Costo vigente de la familia (tpr_producto.costoTM). */
    private BigDecimal costoActual;

    /** Costo sugerido ya guardado para esta familia en la propuesta, si lo hay. */
    private BigDecimal costoGuardado;

    /** La familia ya tiene costo sugerido en la propuesta. */
    private boolean enPropuesta;

    /** Ordenadas por vpp y sucursal, como las listaba el legacy. */
    private List<LineaArmadoDto> lineas = new ArrayList<>();

    /** Una entrada por cada linea fuera de orden, lista para mostrar. */
    private List<String> errores = new ArrayList<>();

    private int lineasNuevas;
    private int lineasActualizadas;
    private int lineasSinCambio;

    /** Lineas de la propuesta cuya lista de precios hoy esta inactiva. */
    private int lineasInactivas;

    /**
     * Las listas INACTIVAS en las que la familia tiene precio, como
     * "Central · Precio 1". No entran en la grilla porque una lista inactiva no se
     * reprecia, igual que en el legacy; se nombran para que su ausencia no parezca un
     * error. Se activan desde "Listas de precio".
     */
    private List<String> listasInactivas = new ArrayList<>();

    /** true solo en la respuesta de {@code guardarFamilia}. */
    private boolean guardado;

    /** Cuantas listas llevan un porcentaje cambiado en el editor. */
    private int porcentajesCambiados;

    /**
     * Solo en la vista previa en lote ({@code calcularFamilias}): por que la familia no se
     * puede guardar tal como esta -inactiva, sin listas activas, fuera de orden, sin nada
     * que cambie-, ya escrito para mostrar. Nulo si se puede guardar. En lote no se corta
     * en la primera familia con problemas: cada una dice el suyo.
     */
    private String impedimento;
}
