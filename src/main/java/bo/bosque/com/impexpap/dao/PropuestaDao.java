package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.PropuestaDto;
import bo.bosque.com.impexpap.dto.PropuestaItemNoCreadoDto;
import bo.bosque.com.impexpap.model.Propuesta;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a <b>tpr_propuesta</b> via procedimientos almacenados
 * ({@code p_abm_propuesta} y {@code p_list_propuesta}) usando {@link SpHelper}.
 * Nada de SQL escrito a mano.
 *
 * <p>Criterio para elegir el overload de {@link SpHelper}:
 * <ul>
 *   <li>Las altas y modificaciones mandan el modelo completo
 *       ({@code ejecutarAbm}): el POJO tiene exactamente las 10 columnas de la
 *       tabla y las 10 existen como parametro del SP.</li>
 *   <li>Las operaciones que solo necesitan uno o dos parametros
 *       (borrar, marcar generacion, buscar por id) usan el overload de Map:
 *       {@code p_list_propuesta} y {@code p_abm_propuesta} tratan
 *       {@code IS NULL} como "sin filtro" y el overload de modelo conserva los
 *       ceros, asi que mandar el modelo entero filtraria de mas.</li>
 * </ul>
 */
@Repository
public class PropuestaDao implements IPropuesta {

    private static final String SP_ABM  = "p_abm_propuesta";
    private static final String SP_LIST = "p_list_propuesta";

    private final SpHelper spHelper;

    public PropuestaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // ---------------------------------------------------------------- ABM ---

    /**
     * ABM generico: {@code p_abm_propuesta} con ACCION 'I', 'U' o 'D'.
     * El model tiene exactamente las 10 columnas de la tabla, asi que se puede
     * mandar entero por {@code ejecutarAbm} (Jackson lo serializa campo a campo
     * y las 10 existen como parametro del SP).
     *
     * @return {@link RespuestaSp}; en 'I' el idPropuesta nuevo viaja en idGenerado
     */
    @Override
    public RespuestaSp registrarPropuesta(Propuesta mb, String acc) {
        return this.spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    /**
     * {@code p_abm_propuesta} ACCION = 'I'. Inserta la propuesta y, en la misma
     * transaccion, su fila de {@code tpr_autorizacion} en estado "en proceso".
     *
     * @return {@link RespuestaSp} con {@code idGenerado} = id nuevo
     *         (SCOPE_IDENTITY, ya no el "ultimo id de la tabla" del legacy)
     */
    @Override
    public RespuestaSp insertar(Propuesta propuesta) {
        // No hace falta limpiar idPropuesta: la rama 'I' del SP no lo lee, el id
        // lo asigna la BD (IDENTITY) y vuelve en @idGenerado.
        return spHelper.ejecutarAbm(SP_ABM, propuesta, "I");
    }

    /**
     * {@code p_abm_propuesta} ACCION = 'U'. Modifica cabecera y auditoria.
     * El SP no actualiza {@code estado} (columna muerta) ni la autorizacion.
     *
     * @return {@link RespuestaSp}; {@code idGenerado} llega en 0
     */
    @Override
    public RespuestaSp actualizar(Propuesta propuesta) {
        return spHelper.ejecutarAbm(SP_ABM, propuesta, "U");
    }

    /**
     * {@code p_abm_propuesta} ACCION = 'D'. Borra la fila de {@code tpr_propuesta}.
     *
     * @return {@link RespuestaSp}; {@code idGenerado} llega en 0
     */
    @Override
    public RespuestaSp eliminar(Long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    /**
     * {@code p_abm_propuesta} ACCION = 'B'. Graba el usuario que genero la
     * propuesta y sella {@code audFecGenerado} con GETDATE() del servidor.
     *
     * @return {@link RespuestaSp}; {@code idGenerado} llega en 0
     */
    @Override
    public RespuestaSp registrarGeneracion(Long idPropuesta, Long audUsGenerado) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("audUsGenerado", audUsGenerado);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "B");
    }

    // ------------------------------------------------------------ LISTADOS ---

    /**
     * {@code p_list_propuesta} ACCION = 'L'.
     *
     * @return todas las propuestas que cumplen el filtro; lista vacia si no hay
     */
    @Override
    public List<Propuesta> listar(Propuesta filtro) {
        // Overload de modelo: todos los campos son wrappers, asi que los que el
        // cliente no mando viajan en null y SpHelper los descarta -> el SP los ve
        // como "sin filtro". No hay riesgo de "0 que filtra" como en otros modulos.
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Propuesta.class);
    }

    /**
     * {@code p_list_propuesta} ACCION = 'L' con {@code @idPropuesta} como unico filtro.
     *
     * @return la propuesta, o {@code null} si el id no existe
     */
    @Override
    public Propuesta obtenerPorId(Long idPropuesta) {
        Map<String, Object> params = new HashMap<>();
        params.put("idPropuesta", idPropuesta);
        List<Propuesta> lst = spHelper.ejecutarListado(SP_LIST, params, "L", Propuesta.class);
        return lst.isEmpty() ? null : lst.get(0);
    }

    /**
     * {@code p_list_propuesta} ACCION = 'B'.
     *
     * @return el id mas alto de la tabla, o {@code null} si esta vacia
     * @deprecated ver {@link IPropuesta#obtenerUltimoId()}: no sirve para saber
     *             que id se acaba de insertar.
     */
    @Override
    @Deprecated
    public Long obtenerUltimoId() {
        List<Propuesta> lst = spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "B", Propuesta.class);
        return lst.isEmpty() ? null : lst.get(0).getIdPropuesta();
    }

    /**
     * {@code p_list_propuesta} ACCION = 'C'.
     *
     * @return cabecera de la propuesta mas {@code esAprobada} de
     *         {@code tpr_autorizacion}, o {@code null} si el id no existe
     */
    @Override
    public PropuestaDto obtenerDetalle(Long idPropuesta) {
        Map<String, Object> params = new HashMap<>();
        params.put("idPropuesta", idPropuesta);
        List<PropuestaDto> lst = spHelper.ejecutarListado(SP_LIST, params, "C", PropuestaDto.class);
        return lst.isEmpty() ? null : lst.get(0);
    }

    /**
     * {@code p_list_propuesta} ACCION = 'D'. Devuelve una sola columna, {@code tipo};
     * se mapea sobre {@link Propuesta} y se lee ese campo (el resto queda en null).
     *
     * @return el tipo, o {@code null} si no hay fila o la columna esta en NULL
     */
    @Override
    public Integer obtenerTipo(Long idPropuesta) {
        Map<String, Object> params = new HashMap<>();
        params.put("idPropuesta", idPropuesta);
        List<Propuesta> lst = spHelper.ejecutarListado(SP_LIST, params, "D", Propuesta.class);
        return lst.isEmpty() ? null : lst.get(0).getTipo();
    }

    /**
     * {@code p_list_propuesta} ACCION = 'E' — articulos no creados, caso por familias.
     * El resultset lo produce un procedimiento REMOTO via OPENROWSET; la forma no
     * es verificable desde esta base (ver {@link PropuestaItemNoCreadoDto}).
     */
    @Override
    public List<PropuestaItemNoCreadoDto> listarItemsNoCreadosPorFamilia(Long idPropuesta) {
        return listarItemsNoCreados(idPropuesta, "E");
    }

    /**
     * {@code p_list_propuesta} ACCION = 'F' — articulos no creados, caso por articulos.
     * Misma advertencia que la rama E.
     */
    @Override
    public List<PropuestaItemNoCreadoDto> listarItemsNoCreadosPorArticulo(Long idPropuesta) {
        return listarItemsNoCreados(idPropuesta, "F");
    }

    private List<PropuestaItemNoCreadoDto> listarItemsNoCreados(Long idPropuesta, String accion) {
        Map<String, Object> params = new HashMap<>();
        params.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, params, accion, PropuestaItemNoCreadoDto.class);
    }

}
