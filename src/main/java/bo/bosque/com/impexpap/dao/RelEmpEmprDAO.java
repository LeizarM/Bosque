package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RelEmplEmpr;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RelEmpEmprDAO implements IRelEmpEmpr {


    /**
     * Procedimiento para obtener el listado de relaciones laborales de un empleado
     * @param codEmpleado
     * @return
     */
    public List<RelEmplEmpr> obtenerRelacionesLaborales( int codEmpleado ) {
        return null;
    }
}
