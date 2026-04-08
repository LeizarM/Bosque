package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TiposCargo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ITiposCargo {

    /**
     * Registrar, actualizar o eliminar un Tipo de Cargo
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrarTiposCargo(TiposCargo mb, String acc);

    /**
     * Obtener tipos de cargo. Si idTipoCargo == 0 devuelve todos.
     * @param idTipoCargo ID a buscar (0 = todos)
     */
    List<TiposCargo> obtenerTiposCargo(long idTipoCargo);
}

