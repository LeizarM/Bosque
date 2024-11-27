package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.PrestamoEstado;

public interface IPrestamoEstado {

    /**
     * Para registrar un nuevo estado de préstamo
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarPrestamoEstado( PrestamoEstado mb, String acc );
}
