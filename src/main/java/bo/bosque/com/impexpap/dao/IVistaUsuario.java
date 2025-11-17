package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Merma;
import bo.bosque.com.impexpap.model.VistaUsuario;

import java.util.List;

public interface IVistaUsuario {


    /**
     * Para registrar la vista por usuario
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarVistaUsuario(VistaUsuario mb, String acc );

    /**
     * Listara los permisos de vistas y botones para un usuario para que posteriormente se pueda armar un arbol
     * @param codUsuario
     * @return
     */
    List<VistaUsuario> lstPermisosVistasYBotones ( int codUsuario );

}
