package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DetalleResmado;

public interface IDetalleResmado {

    /**
     * Para registrar el Detalle Resmado
     * @param regDetResmado
     * @param acc
     * @return true or false
     */
    boolean registrarDetalleResmado(DetalleResmado regDetResmado, String acc );


}
