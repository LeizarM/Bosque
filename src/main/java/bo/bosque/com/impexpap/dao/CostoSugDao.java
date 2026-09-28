package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.model.CostoSug;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de <b>tpr_costoSug</b>. Todo por procedimiento almacenado via
 * {@link SpHelper}; no hay SQL crudo.
 *
 * <p>Los listados usan siempre el overload de {@code Map} y no el de modelo:
 * {@code p_list_costSug} trata {@code @param IS NULL} como "sin filtro", y el
 * overload de modelo conserva los {@code Number} en 0, con lo que un
 * "listame todo" terminaria filtrando por idCosSug = 0 y devolviendo cero filas.
 */
@Repository
public class CostoSugDao implements ICostoSug {

    private static final Logger logger = LoggerFactory.getLogger(CostoSugDao.class);

    /** Nombre del ABM. */
    private static final String SP_ABM  = "p_abm_costoSug";

    /**
     * Nombre del listado. Va sin la "o" de "costo" ({@code costSug}, no
     * {@code costoSug}): asi esta creado en la base y asi lo invoca el JSF.
     * No renombrar.
     */
    private static final String SP_LIST = "p_list_costSug";

    private final SpHelper spHelper;

    public CostoSugDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Accion <b>I</b> de {@code p_abm_costoSug}. El procedimiento resuelve solo si
     * corresponde insertar o actualizar segun exista ya el par
     * (idPropuesta, codigoFamilia).
     *
     * @return {@link RespuestaSp} con el idCosSug afectado en {@code idGenerado}
     */
    @Override
    public RespuestaSp registrar(CostoSug mb) {
        return spHelper.ejecutarAbm(SP_ABM, mb, "I");
    }

    /**
     * Accion <b>U</b> de {@code p_abm_costoSug}: actualiza la fila cuyo
     * {@code idCosSug} viene en el modelo.
     *
     * @return {@link RespuestaSp} con el idCosSug actualizado en {@code idGenerado}
     */
    @Override
    public RespuestaSp actualizar(CostoSug mb) {
        return spHelper.ejecutarAbm(SP_ABM, mb, "U");
    }

    /**
     * Accion <b>D</b> de {@code p_abm_costoSug}: borra TODAS las filas de la
     * propuesta indicada (el procedimiento filtra por idPropuesta, no por PK).
     *
     * <p>Se manda solo lo que la accion necesita, con el overload de Map, para no
     * enviar campos que la rama D no usa.
     *
     * @return {@link RespuestaSp}; {@code idGenerado} no aplica y viene en 0
     */
    @Override
    public RespuestaSp eliminarPorPropuesta(Long idPropuesta, Long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    /**
     * Accion <b>L</b> de {@code p_list_costSug} sin ningun filtro.
     *
     * @return todas las filas de tpr_costoSug
     */
    @Override
    public List<CostoSug> listar() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "L", CostoSug.class);
    }

    /**
     * Accion <b>L</b> filtrando por {@code @idCosSug}.
     *
     * @return la fila encontrada, o null
     */
    @Override
    public CostoSug obtenerPorId(Long idCosSug) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCosSug", idCosSug);
        List<CostoSug> filas = spHelper.ejecutarListado(SP_LIST, filtro, "L", CostoSug.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * Accion <b>L</b> filtrando por {@code @idPropuesta}.
     *
     * @return los costos sugeridos de la propuesta
     */
    @Override
    public List<CostoSug> obtenerPorPropuesta(Long idPropuesta) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", CostoSug.class);
    }

    /**
     * Accion <b>L</b> filtrando por {@code @idPropuesta} y {@code @codigoFamilia},
     * que es la clave con la que el ABM decide entre insertar y actualizar.
     *
     * @return la fila encontrada, o null
     */
    @Override
    public CostoSug obtenerPorPropuestaYFamilia(Long idPropuesta, Integer codigoFamilia) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idPropuesta", idPropuesta);
        filtro.put("codigoFamilia", codigoFamilia);
        List<CostoSug> filas = spHelper.ejecutarListado(SP_LIST, filtro, "L", CostoSug.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * Puente para {@code PrecioController}, que todavia decide la accion afuera y
     * solo espera un booleano. Delega en los metodos nuevos.
     *
     * <p>Se traga la {@link SpBusinessException} y devuelve false para no cambiar
     * el contrato del endpoint viejo; el mensaje real queda en el log. Los nuevos
     * consumidores deben usar los metodos que devuelven {@link RespuestaSp}.
     */
    @Override
    @Deprecated
    public boolean registrarCostoSug(CostoSug costoSug, String acc) {
        try {
            RespuestaSp r;
            if ("D".equalsIgnoreCase(acc)) {
                r = eliminarPorPropuesta(costoSug.getIdPropuesta(), costoSug.getAudUsuario());
            } else if ("U".equalsIgnoreCase(acc)) {
                r = actualizar(costoSug);
            } else {
                r = registrar(costoSug);
            }
            return r.isExitoso();
        } catch (SpBusinessException ex) {
            logger.warn("p_abm_costoSug accion {} rechazada: {}", acc, ex.getMessage());
            return false;
        }
    }
}
