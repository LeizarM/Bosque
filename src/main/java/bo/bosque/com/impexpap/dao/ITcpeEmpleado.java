package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeEmpleado;

/**
 * CRUD de {@code tcpe_empleado}: {@code p_list_tcpe_Empleado} y {@code p_abm_tcpe_Empleado}.
 * El comportamiento común esta en {@link ITcpeCrud}; esta interfaz existe por la regla N:N del módulo.
 */
public interface ITcpeEmpleado extends ITcpeCrud<TcpeEmpleado> {
}
