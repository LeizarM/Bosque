package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TiposTransaccion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ITiposTransaccion {

    /**
     * Registrar, actualizar o eliminar un Tipo de Transacción
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrarTiposTransaccion(TiposTransaccion mb, String acc);

    /**
     * Obtener tipos de transacción. Si idTipoTransaccion == 0 devuelve todos.
     * @param idTipoTransaccion ID a buscar (0 = todos)
     */
    List<TiposTransaccion> obtenerTiposTransaccion(long idTipoTransaccion);
}

