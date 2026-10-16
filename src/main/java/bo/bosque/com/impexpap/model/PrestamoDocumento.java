package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Un renglon de una entrega (tabla {@code tarc_prestamo}): una linea de la caja o la caja completa
 * ({@code codContenido} null). Sigue afuera mientras {@code fechaDevolucion} es null.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoDocumento implements Serializable {

    private long codPrestamo;
    private Long codEntrega;
    private Long codCaja;
    private Long codContenido;
    /** La pone el SP y llega como texto "dd/mm/aaaa hh:mm" (la JVM de produccion corre en UTC). */
    private String fechaDevolucion;
    private String observacionDevolucion;
    private long audUsuario;
    private Date audFecha;

    // Solo lectura (p_list_tarc_Prestamo 'L'); empleado, fecha y nota de la entrega salen de su cabecera.
    private Integer numeroCaja;
    private Long codArchivo;
    private String nombreArchivo;
    private String nombreTipoDocumento;
    private String detalle;
    private String gestion;
    private Long codEmpleado;
    private String nombreEmpleado;
    private String fechaEntrega;
    private String observacionEntrega;
    /** Quien hizo el ultimo registro (la entrega o la devolucion). */
    private String nombreUsuario;
}
