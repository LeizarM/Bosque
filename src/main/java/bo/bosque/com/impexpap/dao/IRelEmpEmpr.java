package bo.bosque.com.impexpap.dao;
import java.util.List;

import bo.bosque.com.impexpap.model.EmpleadoCargo;
import bo.bosque.com.impexpap.model.RelEmplEmpr;



public interface IRelEmpEmpr {

    /**
     * Obtendra el lista de relaciones laborales de una empleado
     * @param codEmpleado
     * @return
     */
    List<RelEmplEmpr> obtenerRelacionesLaborales(int codEmpleado );

    /**
     * Procedimiento para el abm
     * @param empCar
     * @param acc
     * @return
     */
    boolean registrarRelEmpEmpr (RelEmplEmpr empCar, String acc);
    /**
     * Procedimiento para el abm
     * @param RelEmp
     * @param acc
     * @return
     */

    boolean obtenerRelEmpEmpr();
}
