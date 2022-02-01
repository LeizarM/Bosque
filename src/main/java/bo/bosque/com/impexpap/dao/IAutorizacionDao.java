package bo.bosque.com.impexpap.dao;
import java.util.List;

import bo.bosque.com.impexpap.model.Autorizacion;
import bo.bosque.com.impexpap.utils.Tipos;


public interface IAutorizacionDao {

    /**
     * Procedimiento que listara las propuestas
     * @return
     */
    List<Autorizacion> listAutorizacion();


    /**
     * Devolvera una lista de los estados de las propuestas
      * @return
     */
    List<Tipos> lstEstadoPropuestas();
}
