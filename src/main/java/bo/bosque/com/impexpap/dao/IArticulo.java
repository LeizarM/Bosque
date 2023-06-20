package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Articulo;

public interface IArticulo  {

    /**
     * Para registrar el Articulo
     * @param articulo
     * @param acc
     * @return
     */
    boolean registrarArticulo( Articulo articulo, String acc );
}
