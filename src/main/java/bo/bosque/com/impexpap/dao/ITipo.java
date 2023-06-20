package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Tipo;

public interface ITipo {

    /**
     * Para registrar el tipo de papel
     * @param tipo
     * @param acc
     * @return
     */
    boolean registrarTipo ( Tipo tipo, String acc );
}
