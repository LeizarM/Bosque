package bo.bosque.com.impexpap.dao;



import bo.bosque.com.impexpap.model.Dependiente;

import java.util.List;

public interface IDependiente {


    /**
     * Obtendra la lista de dependientes de un empleado
     * @param codEmpleado
     * @return
     */
    List<Dependiente> obtenerDependientes( int codEmpleado );

}
