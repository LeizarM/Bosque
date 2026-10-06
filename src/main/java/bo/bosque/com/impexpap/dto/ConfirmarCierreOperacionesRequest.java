// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\ConfirmarCierreOperacionesRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Cuerpo de POST /tareas-rutinarias/cierre-operaciones/confirmar.
 *
 * <p>{@code fecha}: NULL = hoy (el SP lo defaultea).
 *
 * <p><b>2026-09-10: se quitó la lista {@code traspasos}.</b> La verificación
 * de traspasos dejó de ser parte de Cierre de Operaciones y pasó a la tarea
 * 289. Se eliminó el campo en vez de dejarlo ignorado a propósito: un campo
 * que se sigue aceptando y no hace nada es peor que uno que no existe, porque
 * el cliente cree que está guardando algo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarCierreOperacionesRequest {
    private long idBitTarea;
    private Date fecha;
}
