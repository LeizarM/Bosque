package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Presentacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos del catalogo <b>tpr_presentacion</b> mediante procedimientos
 * almacenados mas {@link SpHelper}. Reemplaza al DAO viejo, que armaba el
 * {@code execute p_abm_presentacion ...} a mano con {@code JdbcTemplate} y
 * tenia tres problemas que aqui NO se replican:
 *
 * <ol>
 *   <li>Devolvia {@code boolean} a partir del rowcount, asi que un
 *       {@code UPDATE} sobre un id inexistente se reportaba como fallo y una
 *       validacion de negocio del proc no se podia distinguir de un error de
 *       conexion. Ahora se devuelve {@link RespuestaSp} con error, mensaje e id
 *       generado.</li>
 *   <li>Capturaba solo {@code BadSqlGrammarException} y se tragaba el resto
 *       imprimiendo con {@code System.out}; una violacion de clave foranea al
 *       eliminar pasaba de largo.</li>
 *   <li>Mandaba {@code ps.setInt} sobre columnas {@code bigint} y nunca enviaba
 *       {@code audFecha}.</li>
 * </ol>
 *
 * <p><b>Por que los listados usan el overload de Map y no el de modelo:</b>
 * {@code p_list_presentacion} trata {@code @parametro IS NULL} como "sin
 * filtro". El overload de modelo de {@code ejecutarListado} conserva los
 * {@code Number} en 0, de modo que un modelo recien construido filtraria por
 * {@code idPresentacion = 0} y devolveria cero filas. Con el Map se manda
 * unicamente lo que el que llama pidio filtrar.
 */
@Repository
public class PresentacionDao implements IPresentacion {

    /** Procedimiento de altas, bajas y modificaciones. */
    private static final String SP_ABM  = "p_abm_presentacion";

    /** Procedimiento de consultas. */
    private static final String SP_LIST = "p_list_presentacion";

    private final SpHelper spHelper;

    public PresentacionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Invoca {@code p_abm_presentacion} con ACCION "I", "U" o "D".
     *
     * <p>Se usa el overload de modelo porque los cinco campos de
     * {@link Presentacion} existen uno a uno como parametros del proc. En el
     * alta el proc fuerza {@code estado = 1} y {@code audFecha = GETDATE()},
     * y devuelve el id nuevo en {@code idGenerado}.
     *
     * @return respuesta del proc; lanza {@code SpBusinessException} si el proc
     *         reporta un error de negocio (por ejemplo, borrar una presentacion
     *         que ya esta asignada a productos)
     */
    @Override
    public RespuestaSp registrarPresentacion(Presentacion mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    /**
     * Invoca {@code p_list_presentacion} ACCION "L" sin ningun filtro.
     *
     * @return todas las presentaciones, activas e inactivas
     */
    @Override
    public List<Presentacion> listarPresentaciones() {
        return listarPresentaciones(null, null, null, null);
    }

    /**
     * Invoca {@code p_list_presentacion} ACCION "L" enviando solo los filtros
     * no nulos; los que no se mandan quedan en el DEFAULT NULL del proc, que es
     * su forma de decir "sin filtro".
     *
     * @return las presentaciones que cumplen los filtros
     */
    @Override
    public List<Presentacion> listarPresentaciones(Long idPresentacion, String presentacion,
                                                   Integer estado, Long audUsuario) {
        // LinkedHashMap: orden estable de los parametros en el EXEC generado.
        Map<String, Object> filtro = new LinkedHashMap<>();
        ponerSiHay(filtro, "idPresentacion", idPresentacion);
        ponerSiHay(filtro, "presentacion",   presentacion);
        ponerSiHay(filtro, "estado",         estado);
        ponerSiHay(filtro, "audUsuario",     audUsuario);

        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Presentacion.class);
    }

    /**
     * Invoca {@code p_list_presentacion} ACCION "L" filtrando por
     * {@code @idPresentacion}.
     *
     * @return la fila encontrada, o null si el id no existe
     */
    @Override
    public Presentacion obtenerPorId(long idPresentacion) {
        List<Presentacion> filas = listarPresentaciones(idPresentacion, null, null, null);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * Invoca {@code p_list_presentacion} ACCION "A": las presentaciones con
     * {@code estado = 1}, para los combos.
     *
     * <p>Esa rama del proc selecciona solo {@code idPresentacion} y
     * {@code presentacion}, asi que el resto de las propiedades del modelo
     * vuelve en null. La rama no admite filtros, por eso se manda un Map vacio.
     *
     * @return las presentaciones activas, solo con id y nombre
     */
    @Override
    public List<Presentacion> listarActivas() {
        return spHelper.ejecutarListado(SP_LIST, new LinkedHashMap<String, Object>(), "A", Presentacion.class);
    }

    /** Agrega el parametro solo si trae valor: null significa "sin filtro" en el proc. */
    private static void ponerSiHay(Map<String, Object> destino, String nombre, Object valor) {
        if (valor != null) {
            destino.put(nombre, valor);
        }
    }
}
