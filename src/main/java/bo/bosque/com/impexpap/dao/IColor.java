package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Color;

public interface IColor {

    /**
     * Para registrar el color
     * @param color
     * @param acc
     * @return
     */
    boolean registrarColor(Color color, String acc );
}
