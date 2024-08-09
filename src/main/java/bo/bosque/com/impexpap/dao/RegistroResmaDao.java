package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.RegistroResmaDetalle;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public class RegistroResmaDao implements IRegistroResmaDetalle {


    /**
     * Metodo para registrar un registro de resma detalle
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarRegistroResmaDetalle(RegistroResmaDetalle mb, String acc) {
        return false;
    }

    /**
     * Metodo para listar los registros de resma detalle
     *
     * @return
     */
    @Override
    public List<RegistroResmaDetalle> lstRegistroResmaDetalle() {
        return null;
    }
}
