package bo.bosque.com.impexpap.dao;
import java.util.List;
import bo.bosque.com.impexpap.model.Telefono;

public interface ITelefono {

    /**
     * Procedimiento para listar los telefonos por persona
     */
    List<Telefono> obtenerTelefonos(int codPersona );
}
