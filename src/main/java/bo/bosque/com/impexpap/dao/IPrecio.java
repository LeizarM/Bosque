package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Precio;

public interface IPrecio {

    /**
     * Para registrar el precio
     * @param precio
     * @param acc
     * @return
     */
    boolean registrarPrecio( Precio precio, String acc );
}
