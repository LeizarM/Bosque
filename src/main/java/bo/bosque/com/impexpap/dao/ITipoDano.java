package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TipoDano;

import java.util.List;

public interface ITipoDano {


    /**
     * Registra un nuevo tipo de daño
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarTipoDano(TipoDano mb, String acc );

    /**
     * Obtiene la lista de tipos de daño
     * @return
     */
    List<TipoDano> lstTipoDano();
}
