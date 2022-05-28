package bo.bosque.com.impexpap.dao;
import java.util.List;
import bo.bosque.com.impexpap.model.RelEmplEmpr;



public interface IRelEmpEmpr {

    /**
     * Obtendra el lista de relaciones laborales de una empleado
     * @param codEmpleado
     * @return
     */
    List<RelEmplEmpr> obtenerRelacionesLaborales(int codEmpleado );
}
