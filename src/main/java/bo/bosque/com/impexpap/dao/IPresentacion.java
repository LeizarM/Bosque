package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Presentacion;

public interface IPresentacion {

    /**
     * Procedimiento para registrar la presentacion
     * @param presentacion
     * @param acc
     * @return
     */
    boolean registrarPresentacion( Presentacion presentacion, String acc );
}
