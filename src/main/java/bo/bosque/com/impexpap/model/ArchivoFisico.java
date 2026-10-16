package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/** Un archivo fisico de documentos (tabla {@code tarc_archivo}): ACHOCALLA, OFICINA CENTRAL. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArchivoFisico implements Serializable {

    private long codArchivo;
    private String nombre;
    /** tb_sucursal donde esta el archivo; null = sin sucursal. */
    private Long codSucursal;
    private Integer estado;
    private long audUsuario;
    private Date audFecha;
    /** El empleado responsable (tb_empleado); obligatorio al guardar. */
    private Long codEmpleado;
    /** Cuando se le asigno: la pone el SP y llega como texto "dd/mm/aaaa hh:mm" (la JVM de produccion corre en UTC). */
    private String fechaAsignacion;

    // Solo lectura (p_list_tarc_Archivo 'L'): no son columnas ni parametros del ABM.
    private String nombreResponsable;
    private String nombreSucursal;
    private String ciudadSucursal;
    private Integer cajas;
    private Integer lineas;
}
