package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.Resmado;

public interface IResmado {


    /**
     * Para registrar el Resmado
     * @param regResmado
     * @param acc
     * @return
     */
    boolean registrarResmado( Resmado regResmado, String acc );
}
