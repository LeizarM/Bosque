package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Un cambio en el historial del costo de una familia: la forma de
 * {@code p_list_bitCostoProducto}, el procedimiento del dialogo "Bitacora de costo /
 * propuesta" del JSF.
 *
 * <p>Cada fila es una aprobacion que le cambio la propuesta aprobada a la familia
 * ({@code tb_bitacora}, campo {@code idPropuestaAprobada}) y, si en esa misma aprobacion
 * tambien cambio el costo, el costo anterior y el nuevo. {@code tb_bitacora} guarda los
 * valores como texto; el DAO los convierte y deja null lo que no es un numero.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class HistorialCostoFamiliaDto {

    private Integer codigoFamilia;

    /** Cuando se aprobo. */
    private Date fecha;

    /** Nombre de quien aprobo; vacio si el usuario ya no tiene empleado asociado. */
    private String usuario;

    private Long audUsuario;

    private Long propuestaAnterior;

    private Long propuestaNueva;

    /** Null si esa aprobacion no cambio el costo. */
    private BigDecimal costoAnterior;

    /** Null si esa aprobacion no cambio el costo. */
    private BigDecimal costoNuevo;

    /** El costo vigente hoy en tpr_producto; se repite en cada fila. */
    private BigDecimal costoActual;

    /** La propuesta aprobada vigente hoy; se repite en cada fila. */
    private Long propuestaActual;
}
