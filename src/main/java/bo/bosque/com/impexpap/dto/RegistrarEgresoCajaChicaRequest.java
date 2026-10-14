// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\RegistrarEgresoCajaChicaRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/** Cuerpo de POST /tareas-rutinarias/caja-chica/registrar-egreso. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEgresoCajaChicaRequest {
    // Ni ocurrencia ni sucursal (archivo SQL 96): el SP la saca del cargo
    // vigente del empleado del token.
    private double montoEg;
    private String descripcion;
    private long codEmpDestino;
    private Integer numFactura;
    private Integer numVale;
    private String moneda;
    /** Archivo SQL 100: sin fecha, el SP usa hoy (la app anterior no la manda). */
    private Date fecha;
}
