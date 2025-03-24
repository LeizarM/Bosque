package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.UsuarioBtn;

import java.util.List;

public interface IUsuarioBtn {


    List<UsuarioBtn> botonesXUsuario( int codUsuario );
}
