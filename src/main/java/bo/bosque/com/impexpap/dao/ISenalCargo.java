// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ISenalCargo.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.SenalCargoDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Cargos por revisar (archivo SQL 82): quién no hace sus tareas, por qué
 * puede ser, y el "Está bien así". Las reglas viven en los SP.
 */
public interface ISenalCargo {

    /** [L] Toda la empresa; con {@code verCalladas}, también las marcadas "Está bien así". */
    List<SenalCargoDto> listar(boolean verCalladas);

    /** [S] Calla la señal de una persona y tarea por unos días. */
    RespuestaSp estaBien(long codEmpleado, long idTarRuti, String obs, long audUsuario);

    /** [D] La vuelve a mostrar. */
    RespuestaSp volverAMostrar(long idSC, long codEmpleado, long audUsuario);
}
