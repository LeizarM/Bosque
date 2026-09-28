package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.RangoGramajeDto;
import bo.bosque.com.impexpap.model.RangoGramaje;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO del catalogo <b>tpr_RangoGramaje</b>.
 *
 * <p>Todo el acceso va por {@link SpHelper}: nada de SQL crudo ni de
 * {@code JdbcTemplate} directo.
 *
 * <p><b>Depende del script</b> {@code src/main/resources/sql/tpr_RangoGramaje.sql},
 * que debe estar aplicado en la base. Ese ALTER hace tres cosas de las que este
 * DAO depende:
 * <ul>
 *   <li>agrega a los dos SPs los parametros de salida
 *       {@code @error / @errormsg / @idGenerado} que {@link SpHelper} espera;</li>
 *   <li>cambia {@code @min} y {@code @max} de INT a DECIMAL(16,2) en los dos SPs
 *       — <b>mientras no se aplique, cualquier alta o modificacion sigue
 *       truncando los decimales</b> y el filtro por min/max del listado nunca
 *       matchea un rango con centavos;</li>
 *   <li>agrega audFecha al final del SELECT de la rama 'L', que hoy no la
 *       devuelve, por lo que ese campo del model llegaria siempre en null.</li>
 * </ul>
 *
 * <p><b>Bugs del DAO viejo que NO se replican:</b> declaraba el id y el usuario
 * como {@code int} y los limites como {@code float}, y los enviaba con
 * {@code ps.setFloat()} contra parametros INT del SP, perdiendo los decimales de
 * un rango como 80,50; nunca mandaba {@code @audFecha}; se tragaba la
 * {@code BadSqlGrammarException} imprimiendo por {@code System.out} y devolvia
 * {@code false}, con lo que el error nunca llegaba al usuario.
 */
@Repository
public class RangoGramajeDao implements IRangoGramaje {

    private static final String SP_ABM  = "p_abm_rangoGramaje";
    private static final String SP_LIST = "p_list_rangoGramaje";

    private final SpHelper spHelper;

    public RangoGramajeDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * ACCION 'I' / 'U' / 'D' de {@code p_abm_rangoGramaje}.
     *
     * <p>Se usa el overload de modelo de {@code ejecutarAbm} porque el model tiene
     * exactamente las 5 columnas de la tabla y las 5 existen como parametro del SP
     * ({@code @idRangoGram, @min, @max, @audUsuario, @audFecha}).
     *
     * @return la respuesta del SP; en el alta {@code idGenerado} trae el
     *         idRangoGram recien creado via SCOPE_IDENTITY().
     */
    @Override
    public RespuestaSp registrarRangoGramaje(RangoGramaje rangoGramaje, String acc) {
        return spHelper.ejecutarAbm(SP_ABM, rangoGramaje, acc);
    }

    /**
     * ACCION 'L' sin filtros: el catalogo completo, crudo.
     */
    @Override
    public List<RangoGramaje> listarRangoGramaje() {
        // Map vacio a proposito: el overload de modelo conserva los Number en 0 y
        // mandaria idRangoGram=0 / min=0 / max=0 como filtros reales. Con el Map
        // vacio el SP usa sus DEFAULT NULL y devuelve todo.
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "L", RangoGramaje.class);
    }

    /**
     * ACCION 'L' filtrando por id.
     *
     * @return la fila, o null si el id no existe. La PK es unica, pero se toma la
     *         primera fila en vez de reventar si el SP llegara a devolver mas.
     */
    @Override
    public RangoGramaje obtenerPorId(Long idRangoGram) {
        Map<String, Object> filtro = new HashMap<String, Object>();
        filtro.put("idRangoGram", idRangoGram);

        List<RangoGramaje> filas =
                spHelper.ejecutarListado(SP_LIST, filtro, "L", RangoGramaje.class);

        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * ACCION 'A': el catalogo con la etiqueta "[ min - max ]" ya armada por el SP.
     */
    @Override
    public List<RangoGramajeDto> listarParaCombo() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "A", RangoGramajeDto.class);
    }

    /**
     * ACCION 'B': los rangos asignados a un grupo de familia SAP y un tipo de papel.
     *
     * <p>Esta rama filtra con igualdad directa (compara la columna
     * idGrpFamiliaSap contra su parametro y idTipo contra el suyo), no con el patron
     * "IS NULL = sin filtro" del resto del modulo: si alguno de los dos llega en
     * null la consulta devuelve vacio. Por eso se validan aqui antes de ir a la
     * base, y no se usa el overload de modelo (que ademas mandaria parametros de
     * mas).
     */
    @Override
    public List<RangoGramajeDto> listarPorGrupoFamiliaYTipo(Long idGrpFamiliaSap, Long idTipo) {
        if (idGrpFamiliaSap == null || idTipo == null) {
            throw new IllegalArgumentException(
                    "La ACCION 'B' de " + SP_LIST + " exige grupo de familia y tipo: no admite filtro vacio.");
        }

        Map<String, Object> filtro = new HashMap<String, Object>();
        filtro.put("idGrpFamiliaSap", aInt(idGrpFamiliaSap));
        filtro.put("idTipo", aInt(idTipo));

        return spHelper.ejecutarListado(SP_LIST, filtro, "B", RangoGramajeDto.class);
    }

    /**
     * Estrecha a {@code int} un id que el resto de la aplicacion transporta como
     * {@code Long}. Los parametros {@code @idGrpFamiliaSap} y {@code @idTipo} de
     * {@code p_list_rangoGramaje} estan declarados INT aunque las columnas PK de
     * tpr_grupoFamiliaSap y tpr_tipo sean bigint. Se concentra la conversion aqui
     * para que el desajuste este en un solo lugar y falle fuerte si algun dia un
     * id se pasa del rango de int, en vez de truncarse en silencio.
     */
    private static Integer aInt(Long id) {
        if (id == null) {
            return null;
        }
        if (id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(
                    "El id " + id + " no entra en el parametro int de " + SP_LIST + ".");
        }
        return Integer.valueOf(id.intValue());
    }
}
