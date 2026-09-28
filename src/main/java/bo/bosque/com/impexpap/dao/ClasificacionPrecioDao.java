package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.ClasificacionPrecioDto;
import bo.bosque.com.impexpap.model.ClasificacionPrecio;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO de tpr_clasificacionPrecio. Todo el acceso es por procedimiento almacenado a traves
 * de {@link SpHelper}; no hay SQL crudo.
 *
 * <p><b>Letras de ACCION del SP historico</b> (documentadas porque no son las estandar):
 * <ul>
 *   <li>p_abm_clasificacionPrecio: <b>'B'</b> = alta (no 'I'), <b>'U'</b> = cambia nombre y
 *       estado, <b>'A'</b> = solo cambia estado, <b>'D'</b> = baja fisica.</li>
 *   <li>p_list_clasificacionPrecio: 'L' = listado plano, 'B' = listado con sucursal,
 *       'C' = vpps registrados, <b>'D'</b> = control de duplicados de vpp (NO es baja).</li>
 * </ul>
 * Hacia afuera este DAO expone I/U/D: {@link #registrarClasificacionPrecio} traduce la 'I'
 * a la 'B' que espera el proc. El ALTER de resources/sql/tpr_ClasificacionPrecio.sql tambien
 * acepta 'I' como sinonimo de 'B', asi que la traduccion es redundante a proposito: el DAO
 * sigue siendo correcto si alguien vuelve a tocar el proc.
 */
@Repository
public class ClasificacionPrecioDao implements IClasificacionPrecio {

    /** Nombre del SP de ABM. */
    private static final String SP_ABM  = "p_abm_clasificacionPrecio";

    /** Nombre del SP de listado. */
    private static final String SP_LIST = "p_list_clasificacionPrecio";

    /** Letra de alta que entiende el SP historico. */
    private static final String ACCION_ALTA_SP = "B";

    private final SpHelper spHelper;

    public ClasificacionPrecioDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * ACCION 'B' (recibiendo "I"), 'U' o 'D' de p_abm_clasificacionPrecio.
     * Manda los campos del model por nombre (ver {@link #parametros}): cada uno tiene su
     * parametro homonimo en el proc, incluido {@code @codSucursal}.
     *
     * @return RespuestaSp; en el alta {@code getIdGenerado()} trae el SCOPE_IDENTITY().
     */
    @Override
    public RespuestaSp registrarClasificacionPrecio(ClasificacionPrecio mb, String acc) {
        String accionSp = "I".equalsIgnoreCase(acc) ? ACCION_ALTA_SP : acc;
        return spHelper.ejecutarAbmMap(SP_ABM, parametros(mb), accionSp);
    }

    /**
     * Los campos del model como parametros de {@code p_abm_clasificacionPrecio}.
     *
     * <p><b>Va por Map y no por {@code ejecutarAbm}.</b> El proc conserva el parametro
     * legacy {@code @idSucursal} -lo manda el JSF, que sigue en produccion- y
     * {@code SimpleJdbcCall} exige un valor para CADA parametro de entrada que ve en la
     * metadata, aunque en el proc tenga DEFAULT: con el model, que no tiene ese campo
     * porque no es una columna, fallaba con "Required input parameter 'idSucursal' is
     * missing". Por Map, el legacy queda en su DEFAULT y el proc usa el nombre nuevo.
     * Los nulos no se mandan: el DEFAULT de esos parametros es NULL, asi que el efecto
     * es el mismo que mandarlos en null.
     */
    static Map<String, Object> parametros(ClasificacionPrecio mb) {
        Map<String, Object> p = new java.util.LinkedHashMap<>();
        ponerSiHay(p, "idClasificacion", mb.getIdClasificacion());
        ponerSiHay(p, "codSucursal", mb.getCodSucursal());
        ponerSiHay(p, "listNum", mb.getListNum());
        ponerSiHay(p, "nombrePrecio", mb.getNombrePrecio());
        ponerSiHay(p, "vpp", mb.getVpp());
        ponerSiHay(p, "estado", mb.getEstado());
        ponerSiHay(p, "audUsuario", mb.getAudUsuario());
        return p;
    }

    private static void ponerSiHay(Map<String, Object> p, String clave, Object valor) {
        if (valor != null) p.put(clave, valor);
    }

    /**
     * ACCION 'A' de p_abm_clasificacionPrecio: activa o desactiva la lista de precios.
     * Se usa el overload de Map para mandar solo los tres parametros que la rama escribe;
     * con el model completo se arrastrarian nombrePrecio, vpp y listNum, que esa rama no usa.
     *
     * @return RespuestaSp sin id generado.
     */
    @Override
    public RespuestaSp cambiarEstado(long idClasificacion, int estado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idClasificacion", idClasificacion);
        params.put("estado", estado);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "A");
    }

    /**
     * ACCION 'D' de p_abm_clasificacionPrecio: baja fisica.
     * El proc rechaza la baja con @error != 0 (y SpBusinessException) si la lista tiene
     * precios o porcentajes asociados.
     *
     * @return RespuestaSp sin id generado.
     */
    @Override
    public RespuestaSp eliminarClasificacionPrecio(long idClasificacion) {
        Map<String, Object> params = new HashMap<>();
        params.put("idClasificacion", idClasificacion);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    /**
     * ACCION 'L' de p_list_clasificacionPrecio.
     * Se arma el Map a mano (y no se manda el model) porque el overload de modelo conserva
     * los Number en 0: un id en 0 se convertiria en un filtro real y devolveria cero filas.
     * Con el Map solo viajan los filtros realmente pedidos.
     *
     * @return filas de la tabla, incluidas listNum y estado que el ALTER agrego al SELECT.
     */
    @Override
    public List<ClasificacionPrecio> obtenerClasificacionPrecio(Long idClasificacion, Long codSucursal) {
        Map<String, Object> filtro = new HashMap<>();
        if (idClasificacion != null && idClasificacion != 0L) filtro.put("idClasificacion", idClasificacion);
        if (codSucursal != null && codSucursal != 0L)         filtro.put("codSucursal", codSucursal);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", ClasificacionPrecio.class);
    }

    /**
     * ACCION 'B' de p_list_clasificacionPrecio: listas de precios con el nombre de la
     * sucursal, ordenadas por estado descendente y vpp. La rama no acepta filtros.
     */
    @Override
    public List<ClasificacionPrecioDto> obtenerClasificacionPrecioConSucursal() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "B", ClasificacionPrecioDto.class);
    }

    /**
     * ACCION 'C' de p_list_clasificacionPrecio: devuelve un resultset de una sola columna
     * (vpp). Se mapea al model y se extrae el unico campo poblado.
     */
    @Override
    public List<Integer> obtenerVpps() {
        List<ClasificacionPrecio> filas =
                spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "C", ClasificacionPrecio.class);
        List<Integer> vpps = new ArrayList<>(filas.size());
        for (ClasificacionPrecio fila : filas) {
            vpps.add(fila.getVpp());
        }
        return vpps;
    }

    /**
     * ACCION 'D' de p_list_clasificacionPrecio: control de duplicados de vpp.
     * OJO: en el proc de LISTADO la 'D' no es baja, es la verificacion de duplicados.
     * El ALTER agrego la exclusion por @idClasificacion para que, al editar, la propia fila
     * no se reporte como duplicada (bug del legacy).
     *
     * @return true si otra lista de precios ya usa ese vpp.
     */
    @Override
    public boolean existeVpp(int vpp, Long idClasificacionExcluir) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("vpp", vpp);
        if (idClasificacionExcluir != null && idClasificacionExcluir != 0L) {
            filtro.put("idClasificacion", idClasificacionExcluir);
        }
        return !spHelper.ejecutarListado(SP_LIST, filtro, "D", ClasificacionPrecio.class).isEmpty();
    }
}
