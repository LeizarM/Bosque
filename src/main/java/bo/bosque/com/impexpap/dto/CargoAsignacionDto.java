// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\CargoAsignacionDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Una fila de asignación cargo↔tarea rutinaria — un dependiente. Se traduce
 * a XML antes de llamar a p_registrar_tac_tareaRutinariaConCargos (ver
 * TareaRutinariaConCargosDao) porque jTDS no soporta table-valued parameters.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CargoAsignacionDto {

    /** Obligatorio. */
    private long codCargo;

    /** NULL = aplica al cargo en TODAS sus sucursales. */
    private Long codCargoSucursal;

    /** NULL = empieza hoy. */
    private Date fechaInicio;

    /** NULL = permanente, sin fecha de fin. */
    private Date fechaFin;
}
