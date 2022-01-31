package bo.bosque.com.impexpap.dao;
import java.util.List;

import bo.bosque.com.impexpap.model.Autorizacion;


public interface IAutorizacionDao {

    /**
     * Procedimiento que listara las propuestas
     * @return
     */
    List<Autorizacion> listAutorizacion();
}
