package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ClasificacionPrecio;

public interface IClasificacionPrecio {

    /**
     * Para registrar el clasificacion precio
     * @param clasificacionPrecio
     * @param acc
     * @return
     */
    boolean registrarClasificacionPrecio( ClasificacionPrecio clasificacionPrecio, String acc );
}
