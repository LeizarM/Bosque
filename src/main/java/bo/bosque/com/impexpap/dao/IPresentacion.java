package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Presentacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Contrato de acceso a datos del catalogo <b>tpr_presentacion</b>.
 *
 * <p>Todo pasa por procedimientos almacenados: {@code p_abm_presentacion} para
 * las altas, bajas y modificaciones, y {@code p_list_presentacion} para las
 * consultas. No hay SQL crudo en ninguna implementacion.
 */
public interface IPresentacion {

    /**
     * Alta, modificacion o baja de una presentacion.
     * Invoca {@code p_abm_presentacion} con la ACCION recibida.
     *
     * @param mb  datos de la presentacion
     * @param acc "I" = insertar, "U" = actualizar, "D" = eliminar
     * @return respuesta del proc; en el alta trae el id nuevo en
     *         {@code getIdGenerado()}
     */
    RespuestaSp registrarPresentacion(Presentacion mb, String acc);

    /**
     * Listado completo del catalogo, activas e inactivas.
     * Invoca {@code p_list_presentacion} ACCION "L" sin filtros.
     */
    List<Presentacion> listarPresentaciones();

    /**
     * Listado filtrado. Cada parametro en null significa "sin filtro".
     * Invoca {@code p_list_presentacion} ACCION "L".
     *
     * @param idPresentacion id exacto, o null
     * @param presentacion   nombre exacto (el proc compara por igualdad, no por LIKE), o null
     * @param estado         1 = activas, 0 = inactivas, null = todas
     * @param audUsuario     usuario de auditoria, o null
     */
    List<Presentacion> listarPresentaciones(Long idPresentacion, String presentacion,
                                            Integer estado, Long audUsuario);

    /**
     * Una presentacion por su id.
     * Invoca {@code p_list_presentacion} ACCION "L" filtrando por @idPresentacion.
     *
     * @return la presentacion, o null si no existe
     */
    Presentacion obtenerPorId(long idPresentacion);

    /**
     * Presentaciones activas (estado = 1), solo id y nombre: es la lista que
     * alimenta los combos de producto.
     * Invoca {@code p_list_presentacion} ACCION "A". Los demas campos del
     * modelo vuelven en null porque esa rama no los selecciona.
     */
    List<Presentacion> listarActivas();
}
