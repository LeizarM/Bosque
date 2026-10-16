package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Una entrega a un empleado (tabla {@code tarc_entrega}); su numero es {@code codEntrega}. Lo que se llevo, cajas
 * completas o documentos de cualquier caja, son sus renglones ({@link PrestamoDocumento}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EntregaDocumento implements Serializable {

    private long codEntrega;
    private Long codEmpleado;
    /** La pone el SP y llega como texto "dd/mm/aaaa hh:mm" (la JVM de produccion corre en UTC). */
    private String fechaEntrega;
    private String observacion;
    private long audUsuario;
    private Date audFecha;

    // Solo lectura (p_list_tarc_Entrega 'L').
    private String nombreEmpleado;
    private Integer cajas;
    private Integer documentos;
    /** Renglones que siguen afuera; 0 = ya volvio todo. */
    private Integer pendientes;
    /** La ultima devolucion, solo cuando ya volvio todo. */
    private String fechaDevolucion;
    /** Quien registro la entrega. */
    private String nombreUsuario;
}
