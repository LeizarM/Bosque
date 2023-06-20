package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Precio;
import org.springframework.stereotype.Repository;

@Repository
public class PrecioDao implements  IPrecio {

    /**
     * Para registrar el precio
     * @param precio
     * @param acc
     * @return
     */
    @Override
    public boolean registrarPrecio(Precio precio, String acc) {
        return false;
    }
}
