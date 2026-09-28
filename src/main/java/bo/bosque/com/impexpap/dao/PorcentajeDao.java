package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.PorcentajeDto;
import bo.bosque.com.impexpap.model.Porcentaje;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacion de {@link IPorcentaje} sobre los procedimientos almacenados
 * {@code p_abm_porcentaje} y {@code p_list_porcentaje}, via {@link SpHelper}.
 *
 * <p>Todos los listados usan el overload de <b>Map</b> y no el de modelo: la
 * rama 'L' del procedimiento trata {@code @param IS NULL} como "sin filtro", y
 * el overload de modelo conserva los Number en 0 — un idPorcen en 0 filtraria
 * de verdad y devolveria cero filas.
 *
 * <p>Requiere el script {@code src/main/resources/sql/tpr_Porcentaje.sql}
 * aplicado: agrega los parametros de salida @error/@errormsg/@idGenerado que
 * lee SpHelper y corrige los alias del listado.
 */
@Repository
public class PorcentajeDao implements IPorcentaje {

    private static final String SP_ABM  = "p_abm_porcentaje";
    private static final String SP_LIST = "p_list_porcentaje";

    private final SpHelper spHelper;

    public PorcentajeDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Invoca p_abm_porcentaje con la ACCION recibida ('I', 'U' o 'D').
     * En 'I' devuelve el idPorcen recien generado en {@code RespuestaSp.idGenerado}.
     */
    @Override
    public RespuestaSp registrarPorcentaje(Porcentaje porcentaje, String acc) {
        return spHelper.ejecutarAbm(SP_ABM, porcentaje, acc);
    }

    /**
     * Invoca p_list_porcentaje ACCION 'L' sin ningun filtro.
     * Devuelve todas las filas de tpr_porcentaje.
     */
    @Override
    public List<Porcentaje> listarPorcentajes() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "L", Porcentaje.class);
    }

    /**
     * Invoca p_list_porcentaje ACCION 'L' enviando unicamente @idPorcen.
     * Devuelve el registro o null si no existe.
     */
    @Override
    public Porcentaje obtenerPorcentajePorId(long idPorcen) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idPorcen", idPorcen);

        List<Porcentaje> lst = spHelper.ejecutarListado(SP_LIST, filtro, "L", Porcentaje.class);
        return lst.isEmpty() ? null : lst.get(0);
    }

    /**
     * Invoca p_list_porcentaje ACCION 'L' enviando solo los filtros no nulos.
     * Devuelve las filas de tpr_porcentaje que cumplen el filtro.
     */
    @Override
    public List<Porcentaje> listarPorcentajesPorFamiliaYClasificacion(Integer codigoFamilia, Long idClasificacion) {
        Map<String, Object> filtro = new HashMap<>();
        if (codigoFamilia != null)   filtro.put("codigoFamilia", codigoFamilia);
        if (idClasificacion != null) filtro.put("idClasificacion", idClasificacion);

        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Porcentaje.class);
    }

    /**
     * Invoca p_list_porcentaje ACCION 'A' con @codigoFamilia.
     * Devuelve la grilla de sucursal + lista de precios + porcentaje vigente;
     * si la familia no tiene porcentajes cargados, idPorcen y porcentaje llegan en 0.
     */
    @Override
    public List<PorcentajeDto> listarPorcentajesPorFamilia(int codigoFamilia) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codigoFamilia", codigoFamilia);

        return spHelper.ejecutarListado(SP_LIST, filtro, "A", PorcentajeDto.class);
    }

    /**
     * Invoca p_list_porcentaje ACCION 'B' sin filtros.
     * Devuelve las sucursales con sus listas de precios activas (estado = 1),
     * sin tocar tpr_porcentaje: idPorcen null y porcentaje en 0.
     */
    @Override
    public List<PorcentajeDto> listarSucursalesParaAbm() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "B", PorcentajeDto.class);
    }

    /**
     * Invoca p_list_porcentaje ACCION 'C' con @codigoFamilia.
     * Devuelve las listas de precios sin porcentaje cargado para esa familia.
     */
    @Override
    public List<PorcentajeDto> listarPorcentajesFaltantes(int codigoFamilia) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codigoFamilia", codigoFamilia);

        return spHelper.ejecutarListado(SP_LIST, filtro, "C", PorcentajeDto.class);
    }
}
