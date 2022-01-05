package bo.bosque.com.impexpap.dao;
import java.util.List;
import bo.bosque.com.impexpap.model.Vista;


public interface IVistaDao {
    /**
     * Procedimiento para obtner el menu por usuario
     * @param codUsuario
     * @return
     */
    List<Vista> obtainMenuXUser (int codUsuario );

    /**
     * Procedimieno para obtener las rutas de las paginas por usuario
     * @param codUsuario
     * @return
     */
    List<Vista> obtainRoutes ( int codUsuario );
}
