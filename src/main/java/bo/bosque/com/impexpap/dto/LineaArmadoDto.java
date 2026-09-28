package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Una lista de precios de la familia dentro del armado: el precio vigente, lo que
 * ya estaba propuesto y lo que resulta del costo nuevo.
 *
 * <p>La lista de precios (sucursal, vpp, nombre, listNum) sale del JOIN con
 * {@code tpr_clasificacionPrecio} que hace {@code p_list_precio 'D'}, no de las
 * copias denormalizadas de {@code tpr_precioPropuesta}, que en la base estan
 * desfasadas en miles de filas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LineaArmadoDto implements Serializable {

    /** La lista quedaria igual: no se escribe. */
    public static final String SIN_CAMBIO = "SIN_CAMBIO";
    /** Todavia no esta en la propuesta y el precio cambia: se inserta. */
    public static final String NUEVA = "NUEVA";
    /** Ya esta en la propuesta: se actualiza con el calculo nuevo. */
    public static final String ACTUALIZA = "ACTUALIZA";
    /** No hay costo con que calcular: solo se muestra lo vigente y lo guardado. */
    public static final String SIN_CALCULO = "SIN_CALCULO";
    /**
     * Estaba en la propuesta, pero su lista de precios hoy esta inactiva. No se recalcula
     * ni se escribe -una lista inactiva no se reprecia-, pero se muestra: al aprobar,
     * {@code p_abm_precioPropuesta 'C'} aplica todas las filas de la propuesta, tambien
     * esta.
     */
    public static final String LISTA_INACTIVA = "LISTA_INACTIVA";

    private Long idPrecio;
    private Long idClasificacion;
    private Long codSucursal;
    private String nombreSucursal;
    private String nombrePrecio;
    private Integer vpp;
    private Long listNum;

    /** Margen de la familia en esta lista (tpr_porcentaje), en puntos. */
    private BigDecimal porcentaje;
    private BigDecimal iva;
    private BigDecimal it;

    /** Flete de la sucursal, en USD por tonelada. */
    private BigDecimal flete;

    /** Precio por tonelada vigente hoy (tpr_precio). */
    private BigDecimal precioActual;

    /** Fila de tpr_precioPropuesta si la lista ya estaba en la propuesta; si no, nulo. */
    private Long idPrecioPropuesto;

    /** Lo que ya estaba propuesto para esta lista. Nulo si no estaba. */
    private BigDecimal precioPropuestoGuardado;

    /** costo x (1 + porcentaje) x (1 + IVA + IT) + flete. Nulo si no hay costo. */
    private BigDecimal precioCalculado;

    /**
     * {@link #NUEVA}, {@link #ACTUALIZA}, {@link #SIN_CAMBIO}, {@link #SIN_CALCULO} o
     * {@link #LISTA_INACTIVA}.
     */
    private String estado;

    /**
     * El precio calculado queda por debajo del de la lista anterior de la misma
     * sucursal. Es la validacion {@code validar()} del legacy; con una sola linea
     * asi la familia no se puede guardar.
     */
    private boolean fueraDeOrden;

    /** El porcentaje que la familia tiene hoy en tpr_porcentaje para esta lista. */
    private BigDecimal porcentajeVigente;

    /**
     * El porcentaje de la linea es uno cambiado en el editor, distinto del vigente: al
     * guardar se registra en tpr_porcentaje.
     */
    private boolean porcentajeCambiado;
}
