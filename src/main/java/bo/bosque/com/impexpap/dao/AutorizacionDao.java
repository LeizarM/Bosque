package bo.bosque.com.impexpap.dao;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.AutorizacionDto;
import bo.bosque.com.impexpap.dto.AutorizacionEmailDto;
import bo.bosque.com.impexpap.model.Autorizacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import bo.bosque.com.impexpap.utils.Tipos;

/**
 * Acceso a datos de <b>tpr_autorizacion</b>. Todo pasa por procedimientos
 * almacenados via {@link SpHelper}: no hay SQL crudo en esta clase.
 *
 * <p>Procedimientos usados: {@code p_abm_autorizacion} y {@code p_list_autorizacion}.
 * Ambos requieren el ALTER de {@code src/main/resources/sql/tpr_Autorizacion.sql}
 * (parametros de salida &#64;error/&#64;errormsg/&#64;idGenerado y alias de columnas
 * mapeables).
 */
@Repository
public class AutorizacionDao implements IAutorizacion {

    private static final String SP_ABM  = "p_abm_autorizacion";
    private static final String SP_LIST = "p_list_autorizacion";

    private final SpHelper spHelper;

    public AutorizacionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // ==================== p_abm_autorizacion ====================

    /**
     * Invoca p_abm_autorizacion con ACCION 'I', 'U' o 'D'.
     * El model tiene exactamente las columnas de la tabla, asi que se puede
     * mandar entero por {@code ejecutarAbm} (Jackson lo serializa campo a campo).
     *
     * @return {@link RespuestaSp}; en 'I' el idAutorizacion nuevo viaja en idGenerado
     */
    @Override
    public RespuestaSp registrarAutorizacion(Autorizacion mb, String acc) {
        return this.spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    /**
     * Invoca p_abm_autorizacion con ACCION 'A' (resolver la propuesta).
     * Se usa el overload de Map porque el SP solo necesita tres parametros y
     * mandar el model completo pisaria idAutorizacion/audFecha sin necesidad.
     *
     * @return {@link RespuestaSp} sin id generado (es un UPDATE)
     */
    @Override
    public RespuestaSp resolverPropuesta(Long idPropuesta, Integer esAprobada, Long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("esAprobada", esAprobada);
        params.put("audUsuario", audUsuario);
        return this.spHelper.ejecutarAbmMap(SP_ABM, params, "A");
    }

    /**
     * Invoca p_abm_autorizacion con ACCION 'B': deja la propuesta En Espera (3)
     * y limpia la fecha de autorizacion.
     *
     * @return {@link RespuestaSp} sin id generado (es un UPDATE)
     */
    @Override
    public RespuestaSp marcarEnEspera(Long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        return this.spHelper.ejecutarAbmMap(SP_ABM, params, "B");
    }

    // ==================== p_list_autorizacion ====================

    /**
     * Invoca p_list_autorizacion con ACCION 'L'.
     * El SP trata {@code @param IS NULL} como "sin filtro", por eso se arma el Map
     * a mano y solo se envian los filtros que llegaron con valor: el overload de
     * modelo conserva los Number en 0 y un id 0 filtraria de verdad.
     *
     * @return filas crudas de tpr_autorizacion
     */
    @Override
    public List<Autorizacion> listarAutorizacion(Long idAutorizacion, Long idPropuesta, Long audUsuario) {
        Map<String, Object> filtro = new HashMap<>();
        if (idAutorizacion != null) filtro.put("idAutorizacion", idAutorizacion);
        if (idPropuesta    != null) filtro.put("idPropuesta", idPropuesta);
        if (audUsuario     != null) filtro.put("audUsuario", audUsuario);
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "L", Autorizacion.class);
    }

    /**
     * Invoca p_list_autorizacion con ACCION 'A': vista principal de propuestas
     * por autorizar (TOP 100, idPropuesta DESC). No recibe filtros.
     *
     * @return filas de la vista principal como {@link AutorizacionDto}
     */
    @Override
    public List<AutorizacionDto> listAutorizacion() {
        return this.spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "A", AutorizacionDto.class);
    }

    /**
     * Invoca p_list_autorizacion con ACCION 'B'.
     *
     * @return correos del usuario indicado
     */
    @Override
    public List<AutorizacionEmailDto> obtenerEmailUsuario(Long audUsuario) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("audUsuario", audUsuario);
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "B", AutorizacionEmailDto.class);
    }

    /**
     * Invoca p_list_autorizacion con ACCION 'C'.
     *
     * @return correos IPX del creador de la propuesta
     */
    @Override
    public List<AutorizacionEmailDto> obtenerEmailPropuesta(Long idPropuesta) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idPropuesta", idPropuesta);
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "C", AutorizacionEmailDto.class);
    }

    /**
     * Invoca p_list_autorizacion con ACCION 'D'.
     *
     * @return correos IPX de los autorizadores del modulo de precios
     */
    @Override
    public List<AutorizacionEmailDto> obtenerEmailAutorizadores() {
        return this.spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "D", AutorizacionEmailDto.class);
    }

    // ==================== catalogo en memoria ====================

    /**
     * Catalogo fijo de estados de propuesta (v_tipos grupo 38). No consulta la base.
     *
     * @return lista de estados
     */
    @Override
    public List<Tipos> lstEstadoPropuestas() {
        return new Tipos().lstEstadoPropuesta();
    }
}
