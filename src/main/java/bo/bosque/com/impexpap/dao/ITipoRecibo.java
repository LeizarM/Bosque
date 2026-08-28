package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TipoRecibo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Catalogo de tipos de talonario (tmto_tipoRecibo). */
public interface ITipoRecibo {

    /**
     * Registrar, actualizar o eliminar un tipo de recibo.
     * El DELETE rebota si el tipo tiene talonarios o esta asignado a un grupo.
     * @param mb  Objeto con los datos del tipo
     * @param acc Accion ('I', 'U', 'D')
     */
    RespuestaSp registrarTipoRecibo(TipoRecibo mb, String acc);

    /** Un tipo por su id. Lista vacia si no existe. */
    List<TipoRecibo> obtenerTipoRecibo(long codTipoRecibo);

    /** Todos los tipos, ordenados por sigla. */
    List<TipoRecibo> listarTipoRecibo();
}
