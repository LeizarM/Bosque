package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.GrupoFamTipoRangoGramDto;
import bo.bosque.com.impexpap.model.GrupoFamTipoRangoGram;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO de la tabla puente <b>tpr_grupoFamTipoRangoGram</b>.
 *
 * <p>Todo el acceso va por {@link SpHelper}: nada de SQL crudo ni de
 * {@code JdbcTemplate} directo.
 *
 * <p><b>Depende del script</b>
 * {@code src/main/resources/sql/tpr_GrupoFamTipoRangoGram.sql}, que debe estar
 * aplicado en la base. Ese ALTER agrega a los dos SPs los parametros de salida
 * {@code @error / @errormsg / @idGenerado} que {@link SpHelper} espera, y ademas
 * hace que la rama {@code 'L'} respete los filtros: <b>mientras el script no se
 * ejecute, la version vieja del SP ignora los parametros y devuelve la tabla
 * entera</b>, con lo cual {@link #obtenerPorGrupoFamilia(Long)} y
 * {@link #obtenerPorClaveNatural(Long, Long)} contestarian de mas.
 *
 * <p>Version anterior de este DAO: armaba a mano
 * {@code "execute @idGrpFamiliaSap=?, ..."} — literalmente sin nombre de
 * procedimiento, con lo cual el alta nunca pudo funcionar — y se tragaba la
 * excepcion devolviendo {@code false}. Ese comportamiento no se replica.
 */
@Repository
public class GrupoFamTipoRangoGramDao implements IGrupoFamTipoRangoGram {

    private static final String SP_ABM  = "p_abm_grupoFamTipoRangoGram";
    private static final String SP_LIST = "p_list_grupoFamTipoRangoGram";

    private final SpHelper spHelper;

    public GrupoFamTipoRangoGramDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * ACCION 'I' / 'U' / 'D' de {@code p_abm_grupoFamTipoRangoGram}.
     *
     * <p>Se usa el overload de modelo de {@code ejecutarAbm} porque el model tiene
     * exactamente las 5 columnas de la tabla y las 5 existen como parametro del SP.
     *
     * @return la respuesta del SP. {@code idGenerado} siempre es 0: la tabla es un
     *         HEAP sin IDENTITY, no hay id que devolver.
     */
    @Override
    public RespuestaSp registrarGrupoFamTipoRangoGram(GrupoFamTipoRangoGram grupoFamTipoRangoGram, String acc) {
        return spHelper.ejecutarAbm(SP_ABM, grupoFamTipoRangoGram, acc);
    }

    /**
     * ACCION 'L' sin filtros: devuelve las filas crudas de la tabla puente.
     */
    @Override
    public List<GrupoFamTipoRangoGram> listarGrupoFamTipoRangoGram() {
        // Map vacio a proposito: el overload de modelo conserva los Number en 0 y
        // mandaria idGrpFamiliaSap=0 como filtro. Aqui no se manda ningun filtro y
        // el SP usa sus DEFAULT NULL.
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "L", GrupoFamTipoRangoGram.class);
    }

    /**
     * ACCION 'L' filtrando por grupo de familia: las asignaciones de ese grupo,
     * una por tipo de papel que tenga configurado.
     */
    @Override
    public List<GrupoFamTipoRangoGram> obtenerPorGrupoFamilia(Long idGrpFamiliaSap) {
        Map<String, Object> filtro = new HashMap<String, Object>();
        filtro.put("idGrpFamiliaSap", aInt(idGrpFamiliaSap));
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", GrupoFamTipoRangoGram.class);
    }

    /**
     * ACCION 'L' filtrando por la clave natural completa.
     *
     * @return la unica fila del par (grupo, tipo), o null si no esta configurado.
     *         El par es unico en la tabla (61 pares sobre 61 filas), pero como no
     *         hay UNIQUE que lo garantice se toma la primera fila en vez de
     *         reventar si algun dia aparece una duplicada.
     */
    @Override
    public GrupoFamTipoRangoGram obtenerPorClaveNatural(Long idGrpFamiliaSap, Long idTipo) {
        Map<String, Object> filtro = new HashMap<String, Object>();
        filtro.put("idGrpFamiliaSap", aInt(idGrpFamiliaSap));
        filtro.put("idTipo", aInt(idTipo));

        List<GrupoFamTipoRangoGram> filas =
                spHelper.ejecutarListado(SP_LIST, filtro, "L", GrupoFamTipoRangoGram.class);

        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * ACCION 'A': la tabla de parametros de gramaje ya pivoteada, un renglon por
     * grupo de familia con las columnas Liviano / Mediano / Pesado.
     */
    @Override
    public List<GrupoFamTipoRangoGramDto> listarParametrosGramaje() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "A", GrupoFamTipoRangoGramDto.class);
    }

    /**
     * Estrecha a {@code int} un id que el resto de la aplicacion transporta como
     * {@code Long}. Las columnas idGrpFamiliaSap / idTipo / idRangoGram son
     * {@code int} en esta tabla aunque sean {@code bigint} en sus tablas padre,
     * y los parametros del SP tambien son INT. Se concentra aqui la conversion
     * para que el desajuste este en un solo lugar y falle fuerte si alguna vez un
     * id se pasa del rango de int en lugar de truncarse en silencio.
     *
     * @return null si el id es null (el SP lo toma como "sin filtro")
     */
    private static Integer aInt(Long id) {
        if (id == null) {
            return null;
        }
        if (id > Integer.MAX_VALUE || id < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(
                    "El id " + id + " no entra en la columna int de tpr_grupoFamTipoRangoGram.");
        }
        return Integer.valueOf(id.intValue());
    }
}
