package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CostoIvaItDto;
import bo.bosque.com.impexpap.model.CostoIvaIt;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO de dbo.tpr_costoIvaIt. Solo procedimientos almacenados via {@link SpHelper};
 * no hay SQL crudo ni jdbcTemplate directo.
 *
 * <p>Los listados arman el Map de filtros a mano en vez de mandar el modelo:
 * {@code ejecutarListado(String, Object, ...)} conserva los Number en 0 y este
 * SP trata 0 como "sin filtro" por defensa, pero con el Map se manda
 * exclusivamente el filtro pedido y no quedan dudas.
 */
@Repository
public class CostoIvaItDao implements ICostoIvaIt {

    private static final String SP_ABM  = "p_abm_costoIvaIt";
    private static final String SP_LIST = "p_list_costoIvaIt";

    private final SpHelper spHelper;

    public CostoIvaItDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * ACCION 'I' / 'U' / 'D' de {@code p_abm_costoIvaIt}.
     *
     * <p>Se manda el modelo completo: sus seis campos son exactamente los seis
     * parametros de entrada del SP. El SP valida el invariante de fila unica
     * ('I' rechaza la segunda fila, 'D' rechaza borrar la ultima) y devuelve el
     * error de negocio en @error/@errormsg, que SpHelper convierte en
     * SpBusinessException.
     *
     * @return error, errormsg e idGenerado del SP
     */
    @Override
    public RespuestaSp registrarCostoIvaIt(CostoIvaIt mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    /**
     * ACCION 'L' sin filtros: todas las filas ordenadas por idCII DESC.
     *
     * @return lista de filas de tpr_costoIvaIt (normalmente una sola)
     */
    @Override
    public List<CostoIvaIt> listarCostoIvaIt() {
        Map<String, Object> sinFiltro = new HashMap<>();
        return spHelper.ejecutarListado(SP_LIST, sinFiltro, "L", CostoIvaIt.class);
    }

    /**
     * ACCION 'L' filtrando por @idCII.
     *
     * @return la fila pedida, o null si no existe
     */
    @Override
    public CostoIvaIt obtenerPorId(int idCII) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCII", idCII);
        List<CostoIvaIt> filas = spHelper.ejecutarListado(SP_LIST, filtro, "L", CostoIvaIt.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * ACCION 'L' filtrando por @idPropuesta.
     *
     * @return filas de esa propuesta (vacia si la propuesta no tiene fila propia)
     */
    @Override
    public List<CostoIvaIt> listarPorPropuesta(long idPropuesta) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", CostoIvaIt.class);
    }

    /**
     * ACCION 'V': la fila vigente resuelta por el SP con
     * {@code TOP 1 ... ORDER BY idCII DESC}, mas totalIvaIt = iva + it.
     *
     * <p>Este es el metodo que debe usar el calculo de precios, en lugar de
     * repetir el {@code SELECT TOP 1} sin ORDER BY del legacy.
     *
     * @return la fila vigente, o null si la tabla esta vacia
     */
    @Override
    public CostoIvaItDto obtenerVigente() {
        Map<String, Object> sinFiltro = new HashMap<>();
        List<CostoIvaItDto> filas =
                spHelper.ejecutarListado(SP_LIST, sinFiltro, "V", CostoIvaItDto.class);
        return filas.isEmpty() ? null : filas.get(0);
    }
}
