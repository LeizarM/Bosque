package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TipoDano;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public class TipoDanoDao implements ITipoDano{


    /**
     * Registra un nuevo tipo de daño
     *
     * @param mb
     * @param acc
     * @return
     */
    @Override
    public boolean registrarTipoDano(TipoDano mb, String acc) {
        return false;
    }

    /**
     * Obtiene la lista de tipos de daño
     *
     * @return
     */
    @Override
    public List<TipoDano> lstTipoDano() {
        return null;
    }
}
