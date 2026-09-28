package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.GrupoFamiliaSap;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de <b>tpr_grupoFamiliaSap</b>. Todo por procedimiento almacenado
 * via {@link SpHelper}; no hay SQL crudo.
 *
 * <p>Reemplaza al antiguo GrupoFamiliaDao, que armaba un
 * <code>execute @idGrpFamiliaSap=?, ...</code> sin nombre de procedimiento
 * (siempre reventaba con BadSqlGrammarException), enviaba los codigos como int
 * contra columnas varchar y se tragaba el error devolviendo false.
 */
@Repository
public class GrupoFamiliaSapDao implements IGrupoFamiliaSap {

    private static final String SP_ABM  = "p_abm_grupoFamiliaSap";
    private static final String SP_LIST = "p_list_grupoFamiliaSap";

    private final SpHelper spHelper;

    public GrupoFamiliaSapDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * ACCION 'I' de p_abm_grupoFamiliaSap.
     *
     * @return RespuestaSp con el id nuevo en {@code getIdGenerado()} (SCOPE_IDENTITY del SP)
     */
    @Override
    public RespuestaSp registrar(GrupoFamiliaSap mb) {
        return spHelper.ejecutarAbm(SP_ABM, mb, "I");
    }

    /**
     * ACCION 'U' de p_abm_grupoFamiliaSap. Actualiza la fila con el idGrpFamiliaSap del modelo.
     *
     * @return RespuestaSp; idGenerado viaja en 0 porque el update no genera id
     */
    @Override
    public RespuestaSp actualizar(GrupoFamiliaSap mb) {
        return spHelper.ejecutarAbm(SP_ABM, mb, "U");
    }

    /**
     * ACCION 'D' de p_abm_grupoFamiliaSap. Se usa el overload de Map para enviar
     * unicamente @idGrpFamiliaSap: el resto de los parametros queda en su DEFAULT
     * y no se toca ningun otro campo.
     *
     * @return RespuestaSp; si la fila esta referenciada por tpr_producto el SP
     *         devuelve el error de integridad en {@code getErrormsg()}
     */
    @Override
    public RespuestaSp eliminar(Long idGrpFamiliaSap) {
        Map<String, Object> params = new HashMap<>();
        params.put("idGrpFamiliaSap", idGrpFamiliaSap);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    /**
     * ACCION 'L' de p_list_grupoFamiliaSap sin filtros.
     *
     * @return todas las filas del catalogo
     */
    @Override
    public List<GrupoFamiliaSap> listar() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "L", GrupoFamiliaSap.class);
    }

    /**
     * ACCION 'L' de p_list_grupoFamiliaSap filtrando solo por id.
     * Map y no modelo: el SP trata @parametro IS NULL como "sin filtro", asi que
     * se manda exclusivamente el id buscado.
     *
     * @return lista con cero o una fila
     */
    @Override
    public List<GrupoFamiliaSap> obtenerPorId(Long idGrpFamiliaSap) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idGrpFamiliaSap", idGrpFamiliaSap);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", GrupoFamiliaSap.class);
    }

    /**
     * ACCION 'L' de p_list_grupoFamiliaSap filtrando por los campos no nulos del modelo.
     * El overload de modelo quita los null y conserva los Number en 0, pero aqui todos
     * los campos numericos son wrappers (Long), asi que un campo sin llenar viaja en
     * null y el SP lo interpreta como "sin filtro".
     *
     * @return filas que cumplen el filtro
     */
    @Override
    public List<GrupoFamiliaSap> buscar(GrupoFamiliaSap filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", GrupoFamiliaSap.class);
    }
}
