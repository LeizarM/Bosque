package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ConfiguracionRecuperacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IConfiguracionRecuperacion {

    List<ConfiguracionRecuperacion> listar();

    ConfiguracionRecuperacion obtener(String clave);

    RespuestaSp actualizar(String clave, String valor, long audUsuario);
}
