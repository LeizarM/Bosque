package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.PrecioPropuesta;

public interface IPrecioPropuesta {

    /**
     * Para registrar el precio propuesta
     * @param precioPropuesta
     * @param acc
     * @return
     */
    boolean registrarPrecioPropuesta( PrecioPropuesta precioPropuesta, String acc );
}
