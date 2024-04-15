package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Merma;

public interface IMerma {

    /**
     * Para registrar la merma
     * @param regMerma
     * @param acc
     * @return
     */
    boolean registrarMerma( Merma regMerma, String acc );
}
