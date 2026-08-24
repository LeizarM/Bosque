package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.PagadoItem;
import bo.bosque.com.impexpap.model.PagadoItemCorte;
import bo.bosque.com.impexpap.model.PagadoItemResumen;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacion contra los dos p_list_ del historico por item.
 * <p>
 * Se usa el overload de Map y no el de modelo: {@code idPagado}, {@code docNum}
 * y {@code origen} en null significan "sin ese filtro" y el overload de modelo
 * borra los nulls, con lo que el parametro nunca llegaria al SP.
 * <p>
 * Los dos SP los crean {@code sql/22_items_congelados_al_pagar.sql} y
 * {@code sql/23_corte_historico.sql}. <b>Tienen que estar corridos en la base
 * antes de desplegar esto</b>: el reporte de comisiones pagadas los consulta.
 */
@Repository
public class PagadoItemDao implements IPagadoItem {

    private static final String SP_LIST       = "p_list_tcom_PagadoItem";
    private static final String SP_LIST_CORTE = "p_list_tcom_PagadoItemCorte";

    private final SpHelper spHelper;

    public PagadoItemDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<PagadoItem> obtenerItems(int mes, int anio, int esInterno,
                                         Long idPagado, Long docNum, String origen,
                                         boolean soloExcluidos) {
        Map<String, Object> p = filtroPeriodo(mes, anio, esInterno, idPagado, docNum, origen);
        p.put("soloExcluidos", soloExcluidos ? 1 : 0);
        return spHelper.ejecutarListado(SP_LIST, p, "L", PagadoItem.class);
    }

    @Override
    public List<PagadoItemResumen> obtenerResumen(int mes, int anio, int esInterno,
                                                  Long idPagado, Long docNum, String origen) {
        // soloExcluidos no se manda: el resumen agrupa por motivo y filtrarlo
        // dejaria afuera la fila DESCONTO, que es contra la que se compara todo
        // lo demas.
        // Los otros tres SI se mandan: el SP los aplica igual en 'L' que en 'R',
        // y un resumen sin origen suma las dos empresas.
        return spHelper.ejecutarListado(SP_LIST,
                filtroPeriodo(mes, anio, esInterno, idPagado, docNum, origen),
                "R", PagadoItemResumen.class);
    }

    @Override
    public List<PagadoItemCorte> obtenerCorte(Integer mes, Integer anio, Integer esInterno) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("mes", mes);
        p.put("anio", anio);
        p.put("esInterno", esInterno);
        return spHelper.ejecutarListado(SP_LIST_CORTE, p, "L", PagadoItemCorte.class);
    }

    /**
     * Los filtros que comparten las dos ACCIONes.
     * <p>
     * Las tres claves puntuales se ponen SIEMPRE, aunque vengan en null: el SP
     * las lee como "sin filtro" y ese es justamente el efecto buscado. Omitirlas
     * del Map daria lo mismo aca, pero dejaria de ser evidente que se estan
     * mandando a proposito.
     */
    private Map<String, Object> filtroPeriodo(int mes, int anio, int esInterno,
                                              Long idPagado, Long docNum, String origen) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("mes", mes);
        p.put("anio", anio);
        p.put("esInterno", esInterno);
        p.put("idPagado", idPagado);
        p.put("docNum", docNum);
        p.put("origen", origen);
        return p;
    }
}
