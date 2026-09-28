package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.PropuestaDto;
import bo.bosque.com.impexpap.dto.PropuestaItemNoCreadoDto;
import bo.bosque.com.impexpap.model.Propuesta;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Acceso a la tabla <b>tpr_propuesta</b>. Todo pasa por procedimientos
 * almacenados: {@code p_abm_propuesta} (acciones I, U, D, B) y
 * {@code p_list_propuesta} (acciones L, B, C, D, E, F).
 */
public interface IPropuesta {

    // ---------------------------------------------------------------- ABM ---

    /**
     * ABM basico de la propuesta. Invoca {@code p_abm_propuesta} con
     * ACCION 'I' (insertar), 'U' (actualizar) o 'D' (eliminar).
     *
     * @param mb  registro con las columnas de tpr_propuesta
     * @param acc "I", "U" o "D"
     * @return respuesta del SP; en 'I' trae el idPropuesta nuevo en idGenerado
     */
    RespuestaSp registrarPropuesta(Propuesta mb, String acc);

    /**
     * Da de alta una propuesta. Invoca {@code p_abm_propuesta} ACCION = 'I'.
     *
     * <p>El procedimiento crea ademas, en la misma transaccion, la fila de
     * {@code tpr_autorizacion} con {@code esAprobada = 0} (en proceso).
     *
     * @param propuesta datos de la propuesta; {@code idPropuesta} se ignora
     * @return respuesta del SP; {@code getIdGenerado()} trae el id nuevo
     */
    RespuestaSp insertar(Propuesta propuesta);

    /**
     * Modifica una propuesta existente. Invoca {@code p_abm_propuesta} ACCION = 'U'.
     * No toca {@code estado} ni la autorizacion asociada.
     *
     * @param propuesta datos a grabar; {@code idPropuesta} es obligatorio
     */
    RespuestaSp actualizar(Propuesta propuesta);

    /**
     * Elimina una propuesta. Invoca {@code p_abm_propuesta} ACCION = 'D'.
     *
     * @param idPropuesta id a borrar
     */
    RespuestaSp eliminar(Long idPropuesta);

    /**
     * Marca quien genero (exporto) la propuesta y sella la fecha con la hora del
     * servidor. Invoca {@code p_abm_propuesta} ACCION = 'B'.
     *
     * @param idPropuesta   propuesta afectada
     * @param audUsGenerado usuario que genera
     */
    RespuestaSp registrarGeneracion(Long idPropuesta, Long audUsGenerado);

    // ------------------------------------------------------------ LISTADOS ---

    /**
     * Lista propuestas. Invoca {@code p_list_propuesta} ACCION = 'L'.
     * Cada campo no nulo del filtro se aplica como igualdad; con el filtro en
     * null (o todos sus campos en null) devuelve todo.
     */
    List<Propuesta> listar(Propuesta filtro);

    /**
     * Una propuesta completa por su id. Invoca {@code p_list_propuesta}
     * ACCION = 'L' filtrando solo por {@code @idPropuesta}.
     *
     * @return la propuesta o {@code null} si no existe
     */
    Propuesta obtenerPorId(Long idPropuesta);

    /**
     * Ultimo id de la tabla. Invoca {@code p_list_propuesta} ACCION = 'B'.
     *
     * @return el id mas alto de {@code tpr_propuesta}, o {@code null} si esta vacia
     * @deprecated para conocer el id recien insertado usar
     *             {@link #insertar(Propuesta)} y su {@code getIdGenerado()}.
     *             Esta rama devuelve el ultimo id de TODA la tabla, no el del
     *             usuario ni el de la sesion: con dos altas simultaneas
     *             devuelve la propuesta del otro.
     */
    @Deprecated
    Long obtenerUltimoId();

    /**
     * Detalle de una propuesta con el estado real del flujo. Invoca
     * {@code p_list_propuesta} ACCION = 'C'.
     *
     * @return el detalle o {@code null} si no existe
     */
    PropuestaDto obtenerDetalle(Long idPropuesta);

    /**
     * Tipo de una propuesta. Invoca {@code p_list_propuesta} ACCION = 'D'.
     *
     * @return el tipo, o {@code null} si la propuesta no existe o lo tiene en NULL
     */
    Integer obtenerTipo(Long idPropuesta);

    /**
     * Articulos no creados en Esppapel y Productiva respecto de Impexpap, caso
     * por familias. Invoca {@code p_list_propuesta} ACCION = 'E'.
     */
    List<PropuestaItemNoCreadoDto> listarItemsNoCreadosPorFamilia(Long idPropuesta);

    /**
     * Articulos no creados en Esppapel y Productiva respecto de Impexpap, caso
     * por articulos. Invoca {@code p_list_propuesta} ACCION = 'F'.
     */
    List<PropuestaItemNoCreadoDto> listarItemsNoCreadosPorArticulo(Long idPropuesta);

}
