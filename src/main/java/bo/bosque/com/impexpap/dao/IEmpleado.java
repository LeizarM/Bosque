package bo.bosque.com.impexpap.dao;
import bo.bosque.com.impexpap.model.Empleado;

import java.util.List;

public interface IEmpleado {

    /**
     * Procedimiento para obtener la lista de empleados
     * @return
     */
    List<Empleado> obtenerEmpleados ( int esActivo );

}
