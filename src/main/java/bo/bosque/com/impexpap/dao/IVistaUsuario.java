package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Merma;
import bo.bosque.com.impexpap.model.VistaUsuario;

public interface IVistaUsuario {


    /**
     * Para registrar la vista por usuario
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarVistaUsuario(VistaUsuario mb, String acc );

}
