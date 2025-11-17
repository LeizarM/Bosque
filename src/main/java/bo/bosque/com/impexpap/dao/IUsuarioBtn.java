package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.UsuarioBtn;

import java.util.List;

public interface IUsuarioBtn {


    List<UsuarioBtn> botonesXUsuario( int codUsuario );

    /**
     * Para registrar un botón en la base de datos
     * @param usuarioBtn
     * @param acc
     * @return
     */
    boolean registroBoton( UsuarioBtn usuarioBtn, String acc );
}
