// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\ObtenerDeHoyRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Cuerpo de los paneles de la revisión de Cierre de Operaciones (arqueos,
 * llegadas, cheques, tareas del día) y de sus PDF. {@code todasSucursales}:
 * replica el checkbox real "Mostrar otras sucursales" del legacy — el default
 * (false) muestra solo la sucursal de la ocurrencia, resuelta server-side de
 * {@code idBitTarea}.
 *
 * <p>El nombre es de cuando los paneles eran solo de hoy. {@code fecha} es el
 * día que se revisa (el "Desplegar" del sistema anterior); null = hoy.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ObtenerDeHoyRequest {
    private long idBitTarea;
    private boolean todasSucursales;
    private Date fecha;
}
