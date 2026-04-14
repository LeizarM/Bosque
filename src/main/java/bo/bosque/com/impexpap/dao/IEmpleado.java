package bo.bosque.com.impexpap.dao;
import bo.bosque.com.impexpap.dto.DescuentoEmpleadoDTO;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.EmpleadoCargo;
import bo.bosque.com.impexpap.model.Persona;

import java.util.Date;
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
    /**
     * MODULO EMPLEADOS RRHH - OBTENDRA UNA LISTA DE EMPLEADOS
     */
    List<Empleado> obtenerLstEmpleados (String search, Integer esActivo,int pageNumber,int pageSize,Integer codEmpresa );
    /**
     * Obtendra el ultimo cargo de un empleado
     */
    Empleado obtenerEmpleadoCargo (int codEmpleado);
    /**
     * Obtendra el ultimo cargo de un empleado
     */
    List<Empleado> obtenerCargosEmpleado (int codEmpleado);
    /**
     * procedimiento para verificar el cargo duplicado al editar
     */
    Empleado verificarCargoDuplicado (int codEmpleado, Date fechaInicio);
    /**
     * procedimiento para verificar el cargo duplicado al editar
     */
    Empleado obtenerFechaInicioUltimoCargo (int codEmpleado, Date fechaInicio);
    /**
     * MODULO EMPLEADOS RRHH - OBTENDRA UNA LISTA DE CARGOS X EMPRESA
     */
    List<Empleado> obtenerCargosXEmpresa (String search,Integer codEmpresaPlanilla );

    /**
     * obtendra el haber basico de un empleado
     * @param codEmpleado
     * @return
     */
    Empleado obtenerHaberBasico (int codEmpleado);

    /**
     * Procedimiento para obtener los prestamos, anticipos y multas de un empleado
     * @param mes
     * @param anio
     * @param codEmpleado
     * @return
     */
    List<DescuentoEmpleadoDTO> obtenerPrestamosAnticiposYMultasEmpleado(int mes, int anio, int codEmpleado );

}
