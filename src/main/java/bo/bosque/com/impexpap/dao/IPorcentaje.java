package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Porcentaje;

public interface IPorcentaje {

    /**
     * Para registrar el porcentaje
     * @param porcentaje
     * @param acc
     * @return
     */
    boolean registrarPorcentaje ( Porcentaje porcentaje, String acc );
}
