// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\BitacoraFiltroRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Cuerpo de los endpoints de /tareas-rutinarias/bitacora/*.
 *
 * <p><b>No trae a quién puede ver quien pregunta.</b> El alcance (su equipo, o
 * todo si tiene {@code btnEmpAll}) lo resuelve el servidor desde el token. Si
 * viajara en el cuerpo, cualquiera podría pedir la bitácora de toda la empresa
 * cambiando un campo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraFiltroRequest {

    // ---- cumplimiento y resumen de generación ----
    private Date fechaIni;
    private Date fechaFin;
    private Long codSucursal;
    private Long codCargo;
    private Long codEmpleado;
    private Long idTarRuti;

    /** R, N, A o P. Nulo = todas. */
    private String cumplimiento;

    // ---- por qué no le llegó ----
    /** Fecha de presentación por la que se pregunta. Nula = hoy. */
    private Date fecha;
}
