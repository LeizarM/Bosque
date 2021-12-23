package bo.bosque.com.impexpap.dao;
import java.util.List;
import bo.bosque.com.impexpap.model.Vista;


public interface IVistaDao {
    /**
     * Procedimiento para obtner el usuario
     * @param codUsuario
     * @return
     */
    List<Vista> obtainMenuXUser (int codUsuario );
}
