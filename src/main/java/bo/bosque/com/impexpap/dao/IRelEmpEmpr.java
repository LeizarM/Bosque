package bo.bosque.com.impexpap.dao;
import java.util.Date;
import java.util.List;

import bo.bosque.com.impexpap.dto.AgendaEmpleadoDto;
import bo.bosque.com.impexpap.model.Empleado;
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
    /**
     * Obtendra la relacion laboral activa de un empleado
     * @param codEmpleado
     * @return
     */
    List<RelEmplEmpr> obtenerRelLab(int codEmpleado );
    /**
     * Procedimiento para obtener el ultimo codRelEmplEmpr (relacion laboral registrada)
     */
    RelEmplEmpr obtenerUltimoCodRelEmplEmpr (int codEmpleado);

    /**
     * {@code p_list_RelEmplEmpr 'G'}: la agenda del Dashboard. Empleados con relación vigente, su cargo
     * y sucursal actuales, sus teléfonos corporativos y si {@code codEmpleadoConsulta} puede abrir su
     * ficha (misma regla que la ficha del trabajador).
     */
    List<AgendaEmpleadoDto> agenda(int codEmpleadoConsulta);
}
