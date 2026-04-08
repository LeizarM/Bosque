package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TiposCambio;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ITiposCambio {

    /**
     * Registrar, actualizar o eliminar un Tipo de Cambio
     * @param mb   Objeto con los datos del tipo de cambio
     * @param acc  Acción ('I', 'U', 'D')
     * @return RespuestaSp con error, errormsg e idGenerado
     */
    RespuestaSp registrarTiposCambio(TiposCambio mb, String acc);

    /**
     * Obtener tipos de cambio. Si idTipoCambio == 0 devuelve todos.
     * @param idTipoCambio ID a buscar (0 = todos)
     * @return Lista de TiposCambio
     */
    List<TiposCambio> obtenerTiposCambio(long idTipoCambio);

    /**
     * Obtener tipos de cambio filtrados por banco
     * @param codBanco Código de banco
     * @return Lista de TiposCambio
     */
    List<TiposCambio> obtenerTiposCambioPorBanco(int codBanco);
}

