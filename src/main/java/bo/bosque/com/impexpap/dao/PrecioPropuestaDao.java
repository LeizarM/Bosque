package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.PrecioPropuestaADto;
import bo.bosque.com.impexpap.dto.PrecioPropuestaBDto;
import bo.bosque.com.impexpap.dto.PrecioPropuestaDDto;
import bo.bosque.com.impexpap.model.PrecioPropuesta;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a tpr_precioPropuesta. Todo pasa por procedimientos almacenados via
 * {@link SpHelper}: p_abm_precioPropuesta y p_list_precioPropuesta. Sin SQL crudo.
 *
 * <p><b>Por que casi todo usa el overload de Map y no el de modelo:</b>
 * <ul>
 *   <li>p_list_precioPropuesta usa {@code IS NULL} como "sin filtro", asi que un 0 seria un
 *       filtro real y devolveria cero filas.</li>
 *   <li>Los nombres de los parametros del listado NO coinciden con las columnas:
 *       {@code @idPrecioPropuesta} (la columna es idPrecioPropuesto), {@code @montoActual}
 *       (columna precioActual) y {@code @montoPropuesto} (columna precioPropuesto). Ademas el
 *       listado no tiene {@code @codSucursal}, {@code @listNum}, {@code @nombrePrecio} ni
 *       {@code @vpp}. Mandar el modelo entero arma un EXEC con parametros inexistentes y
 *       SQL Server lo rechaza.</li>
 * </ul>
 * El ABM si acepta el modelo completo, una vez aplicado el ALTER que agrega el parametro
 * alias {@code @idPrecioPropuesto}.
 */
@Repository
public class PrecioPropuestaDao implements IPrecioPropuesta {

    private static final String SP_ABM  = "p_abm_precioPropuesta";
    private static final String SP_LIST = "p_list_precioPropuesta";

    private final SpHelper spHelper;

    public PrecioPropuestaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // --------------------------- ABM ---------------------------

    /**
     * Delega en insertar/actualizar/eliminar segun {@code acc}. Es el punto de entrada que
     * usa el controller, que decide I o U segun venga o no el id.
     */
    @Override
    public RespuestaSp registrarPrecioPropuesta(PrecioPropuesta mb, String acc) {
        if ("I".equalsIgnoreCase(acc)) return insertar(mb);
        if ("U".equalsIgnoreCase(acc)) return actualizar(mb);
        if ("D".equalsIgnoreCase(acc)) {
            Long id = (mb != null) ? mb.getIdPrecioPropuesto() : null;
            if (id == null) throw new IllegalArgumentException("Falta idPrecioPropuesto para eliminar.");
            return eliminar(id);
        }
        throw new IllegalArgumentException("Accion no soportada para tpr_precioPropuesta: " + acc);
    }

    /**
     * ACCION 'I' de p_abm_precioPropuesta: inserta la fila completa.
     * Devuelve {@link RespuestaSp} con el id nuevo en {@code idGenerado} (SCOPE_IDENTITY).
     *
     * <p>El SP graba audFecha con GETDATE(), ignorando el valor que se mande.
     */
    @Override
    public RespuestaSp insertar(PrecioPropuesta mb) {
        return spHelper.ejecutarAbmMap(SP_ABM, parametros(mb), "I");
    }

    /**
     * ACCION 'U': actualiza todas las columnas de la fila indicada por idPrecioPropuesto.
     * Es un update total: los campos que vengan en null se graban en null.
     */
    @Override
    public RespuestaSp actualizar(PrecioPropuesta mb) {
        return spHelper.ejecutarAbmMap(SP_ABM, parametros(mb), "U");
    }

    /**
     * Los campos del model como parametros de {@code p_abm_precioPropuesta}, por nombre.
     *
     * <p><b>Va por Map y no por {@code ejecutarAbm}.</b> El proc conserva el parametro
     * legacy {@code @idPrePropuesto} -lo manda el JSF, que sigue en produccion- y
     * {@code SimpleJdbcCall} exige un valor para CADA parametro de entrada que ve en la
     * metadata, aunque en el proc tenga DEFAULT: con el model, que no tiene ese campo
     * porque no es una columna, fallaba con "Required input parameter 'idPrePropuesto' is
     * missing". Por Map, el legacy queda en su DEFAULT y el proc usa el nombre nuevo.
     * Los nulos no se mandan: el DEFAULT de esos parametros es NULL, asi que el efecto
     * es el mismo que mandarlos en null.
     */
    static Map<String, Object> parametros(PrecioPropuesta mb) {
        Map<String, Object> p = new LinkedHashMap<>();
        ponerSiHay(p, "idPrecioPropuesto", mb.getIdPrecioPropuesto());
        ponerSiHay(p, "idPropuesta", mb.getIdPropuesta());
        ponerSiHay(p, "idPrecio", mb.getIdPrecio());
        ponerSiHay(p, "codigoFamilia", mb.getCodigoFamilia());
        ponerSiHay(p, "precioActual", mb.getPrecioActual());
        ponerSiHay(p, "precioPropuesto", mb.getPrecioPropuesto());
        ponerSiHay(p, "porcentaje", mb.getPorcentaje());
        ponerSiHay(p, "codSucursal", mb.getCodSucursal());
        ponerSiHay(p, "listNum", mb.getListNum());
        ponerSiHay(p, "nombrePrecio", mb.getNombrePrecio());
        ponerSiHay(p, "vpp", mb.getVpp());
        ponerSiHay(p, "audUsuario", mb.getAudUsuario());
        return p;
    }

    private static void ponerSiHay(Map<String, Object> p, String clave, Object valor) {
        if (valor != null) p.put(clave, valor);
    }

    /** ACCION 'D': borra fisicamente la fila. */
    @Override
    public RespuestaSp eliminar(long idPrecioPropuesto) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPrecioPropuesto", idPrecioPropuesto);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    /**
     * ACCION 'B': actualiza SOLO precioPropuesto. Va por Map a proposito: con el modelo
     * completo se mandarian los demas campos en null y, aunque esta rama no los use, el
     * dia que alguien la amplie estaria pisando datos sin querer.
     */
    @Override
    public RespuestaSp actualizarPrecioPropuesto(long idPrecioPropuesto, BigDecimal precioPropuesto) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPrecioPropuesto", idPrecioPropuesto);
        params.put("precioPropuesto", precioPropuesto);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "B");
    }

    /**
     * ACCION 'C': aplica la propuesta completa. Copia cada precioPropuesto a
     * tpr_precio.precio y el costo sugerido a tpr_producto.costoTM + idPropuestaAprobada.
     * Escribe en tablas que no son la propia: usarla solo desde el flujo de aprobacion.
     */
    @Override
    public RespuestaSp aplicarPreciosDePropuesta(long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "C");
    }

    /**
     * ACCION 'E': re-sincroniza el porcentaje de las filas de una propuesta y una familia
     * con tpr_porcentaje. Actualiza tambien audUsuario y audFecha de esas filas.
     */
    @Override
    public RespuestaSp sincronizarPorcentaje(long idPropuesta, String codigoFamilia, long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("codigoFamilia", codigoFamilia);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "E");
    }

    // ------------------------- Listados -------------------------

    /**
     * ACCION 'L': listado crudo de tpr_precioPropuesta. Solo viajan los campos no nulos del
     * filtro, traducidos a los nombres de parametro del SP (que no son los de las columnas).
     * Con el filtro vacio devuelve la tabla entera.
     */
    @Override
    public List<PrecioPropuesta> listar(PrecioPropuesta filtro) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (filtro != null) {
            ponerSiNoEsNulo(params, "idPrecioPropuesta", filtro.getIdPrecioPropuesto());
            ponerSiNoEsNulo(params, "idPropuesta",       filtro.getIdPropuesta());
            ponerSiNoEsNulo(params, "idPrecio",          filtro.getIdPrecio());
            ponerSiNoEsNulo(params, "codigoFamilia",     filtro.getCodigoFamilia());
            ponerSiNoEsNulo(params, "montoActual",       filtro.getPrecioActual());
            ponerSiNoEsNulo(params, "montoPropuesto",    filtro.getPrecioPropuesto());
            ponerSiNoEsNulo(params, "porcentaje",        filtro.getPorcentaje());
            ponerSiNoEsNulo(params, "audUsuario",        filtro.getAudUsuario());
            ponerSiNoEsNulo(params, "audFecha",          filtro.getAudFecha());
        }
        return spHelper.ejecutarListado(SP_LIST, params, "L", PrecioPropuesta.class);
    }

    /** ACCION 'L' por PK. Devuelve null si no hay fila. */
    @Override
    public PrecioPropuesta obtenerPorId(long idPrecioPropuesto) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPrecioPropuesta", idPrecioPropuesto); // nombre del parametro en el SP
        List<PrecioPropuesta> filas = spHelper.ejecutarListado(SP_LIST, params, "L", PrecioPropuesta.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** ACCION 'L' filtrando por propuesta. */
    @Override
    public List<PrecioPropuesta> listarPorPropuesta(long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, params, "L", PrecioPropuesta.class);
    }

    /**
     * ACCION 'A': detalle de la propuesta con articulo, proveedor, grupo de familia,
     * sucursal, vpp real (el de tpr_clasificacionPrecio) y la formula de calculo armada.
     */
    @Override
    public List<PrecioPropuestaADto> listarDetallePorPropuesta(long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, params, "A", PrecioPropuestaADto.class);
    }

    /**
     * ACCION 'B': comparativo horizontal precios actuales vs propuestos.
     *
     * <p>Se manda el mismo id en los dos parametros a proposito: en el SP publicado hoy la
     * mitad de "precios actuales" filtra por {@code @idPrecioPropuesta} y la de "precios
     * propuestos" por {@code @idPropuesta} - un bug del legacy. Mandando los dos, el listado
     * sale completo con el SP viejo y sigue saliendo igual con el SP ya corregido (el ALTER
     * unifica las dos mitades en COALESCE(@idPropuesta, @idPrecioPropuesta)).
     */
    @Override
    public List<PrecioPropuestaBDto> listarComparativo(long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("idPrecioPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, params, "B", PrecioPropuestaBDto.class);
    }

    /**
     * ACCION 'C': devuelve las filas (a lo sumo una por precio) que ya existen para ese
     * precio en esa propuesta. Se reusa el model porque la rama devuelve una unica columna,
     * idPrecio, que existe en el.
     */
    @Override
    public boolean existePrecioEnPropuesta(long idPropuesta, long idPrecio) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("idPrecio", idPrecio);
        return !spHelper.ejecutarListado(SP_LIST, params, "C", PrecioPropuesta.class).isEmpty();
    }

    /**
     * ACCION 'D': filas de una propuesta para una familia. El SP compara el codigoFamilia
     * contra tpr_precio.codigoFamilia (int), no contra el varchar(15) de la tabla propia.
     */
    @Override
    public List<PrecioPropuestaDDto> listarPorPropuestaYFamilia(long idPropuesta, String codigoFamilia) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("codigoFamilia", codigoFamilia);
        return spHelper.ejecutarListado(SP_LIST, params, "D", PrecioPropuestaDDto.class);
    }

    /** Agrega el parametro solo si tiene valor: el SP trata NULL como "sin filtro". */
    private static void ponerSiNoEsNulo(Map<String, Object> params, String nombre, Object valor) {
        if (valor != null) {
            params.put(nombre, valor);
        }
    }
}
