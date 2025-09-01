package bo.bosque.com.impexpap.dao;



import bo.bosque.com.impexpap.model.Dependiente;
import bo.bosque.com.impexpap.utils.Tipos;

import java.util.List;

public interface IDependiente {


    /**
     * Obtendra la lista de dependientes de un empleado
     * @param codEmpleado
     * @return
     */
    List<Dependiente> obtenerDependientes( int codEmpleado );

    /**
     * Para el abm de dependientes
     * @param dep
     * @param acc
     * @return true si lo hizo correctamente
     */
    boolean registrarDependiente( Dependiente dep, String acc );

    /**
     * Obtendra la lista de dependientes que son empleados
     * @param codEmpleado
     * @return
     */
    List<Dependiente> obtenerDepEmp (int codEmpleado);
    /**
     * Procedimiento para obtener el tipos de parentesco
     * @return
     */
    List<Tipos>lstTipoDependiente();
    /**
     * Procedimiento para obtener el tipos de activo
     * @return
     */
    List<Tipos>lstTipoActivo();


}
