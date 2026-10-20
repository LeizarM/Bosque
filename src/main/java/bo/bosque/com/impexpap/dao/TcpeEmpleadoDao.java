package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeEmpleado;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_empleado} vía {@code p_list_tcpe_Empleado} y {@code p_abm_tcpe_Empleado}.
 * La PK no es IDENTITY: sin código el SP asigna el siguiente; con código {@code guardar} decide I/U según exista.
 */
@Repository
public class TcpeEmpleadoDao extends TcpeCrudDao<TcpeEmpleado> implements ITcpeEmpleado {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codEmpleado" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = {  };

    public TcpeEmpleadoDao(SpHelper spHelper) {
        super(spHelper, TcpeEmpleado.class, "empleado", "codEmpleado", false, FILTROS, COLUMNAS);
    }
}
