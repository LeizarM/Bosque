package bo.bosque.com.impexpap.dao;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.Persona;

import java.util.List;

public interface IEmpleado {

    /**
     * Procedimiento para obtener la lista de empleados
     * @return
     */
    List<Empleado> obtenerListaEmpleados ( int esActivo );

    /**
     * Procedimiento que obtendra al empleado por codigo
     * @return
     */
    Empleado obtenerEmpleado ( int codEmpleado );

    /**
     * Procedimiento para el Abm de la informacion de empleado
     * @param emp
     * @param acc
     * @return
     */
    boolean registroEmpleado ( Empleado emp, String acc );

    /**
     * Procedimiento para obtener el ultimo codigo de empleado insertado
     * @return
     */
    int obtenerUltimoEmpleado( );

    /**
     * Procedimiento para mostrar lista de empleados y dependientes
     * @return
     */
    List <Empleado>obtenerListaEmpleadoyDependientes (int codEmpleado);
    /**
     * Procedimiento para mostrar lista de empleados y dependientes
     * @return
     */
    //List <Empleado>obtenerInfoEmp();
    /**
     * Procedimiento para mostrar datos personales de empleado
     * @return
     */
    List<Empleado>obtenerDatosPerEmp(int codEmpleado);

    /**
     * Procedimiento para mostrar lista de empleados y dependientes
     * @return
     */
    List <Empleado>listaCumpleEmpleado();
    /**
     * Procedimiento para mostrar datos personales de empleado
     * @return
     */
    Empleado obtenerDatosEmpleado(int codEmpleado);
    /**
     * Procedimiento para mostrar datos personales de un empleado segun jerarquia
     * @return
     */
    Empleado obtenerInfoEmpleado(int codEmpleado,int codEmpleadoConsultado);

    /**
     * Procedimiento para mostrar lista de empleados
     * @return
     */
    List <Empleado>lisEmpleados();


}
