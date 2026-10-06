// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\DiagnosticoGeneracionDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Por qué a una persona le llegó o no una tarea en una fecha
 * ({@code p_list_tac_BitTareaRuti @ACCION='P'}, archivo SQL 55).
 *
 * <p>Una fila por tarea que alguno de sus cargos tiene o tuvo asignada. Si la
 * tarea le puede llegar por varios caminos, es el que más lejos llega.
 *
 * <p>{@code filtro}:
 * <ul>
 *   <li>-2 la persona no es del equipo de quien pregunta (una sola fila)</li>
 *   <li>-1 ninguno de sus cargos tuvo esa tarea (una sola fila)</li>
 *   <li> 0 pasó todos los filtros; {@code motivo} dice si se generó, si la
 *          frecuencia no vence ese día o si el Job todavía no corrió</li>
 *   <li>1 a 6 el primer filtro del generador que la dejó afuera</li>
 * </ul>
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticoGeneracionDto {

    private Long idTarRuti;
    private String tarea;
    private Long idFrec;
    private String frecuencia;
    private Long codCargo;
    private String cargo;
    private String sucursal;
    private Integer filtro;
    private String motivo;
    private Boolean existeOcurrencia;
    private Long idBitTarea;
    private Integer fueRealizado;
}
