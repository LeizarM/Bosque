// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\RegistrarEgresoCajaChicaRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/** Cuerpo de POST /tareas-rutinarias/caja-chica/registrar-egreso. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarEgresoCajaChicaRequest {
    private long idBitTarea;
    // codSucursal ya NO viaja aquí — el proc lo resuelve del cargo vigente
    // del empleado dueño de la ocurrencia (paso 20, code-review 2026-09-03).
    private double montoEg;
    private String descripcion;
    private long codEmpDestino;
    private Integer numFactura;
    private Integer numVale;
    private String moneda;
}
