package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.UsuarioBloqueado;

public interface IUsuarioBloqueado {

    UsuarioBloqueado obtenerPorUsuario(int codUsuario);

    boolean registrarAdvertencia(UsuarioBloqueado ub, String acc);

    boolean eliminarAdvertencia (int codUsuario);

}
