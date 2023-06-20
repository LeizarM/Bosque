package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CostoSug;

public interface ICostoSug {


    /**
     * Registrar el costo sugerido
     * @param costoSug
     * @param acc
     * @return
     */
    boolean registrarCostoSug( CostoSug costoSug, String acc );
}
