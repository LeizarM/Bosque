package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ProveedorExtSap;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos del catalogo <b>tpr_proveedorExtSap</b> mediante procedimientos
 * almacenados mas {@link SpHelper}. Reemplaza al DAO viejo, que armaba el
 * {@code execute p_abm_proveedorExtSap ...} a mano con {@code JdbcTemplate} y
 * tenia cuatro problemas que aqui NO se replican:
 *
 * <ol>
 *   <li>Devolvia {@code boolean} a partir del rowcount: un {@code UPDATE} sobre
 *       un id inexistente se reportaba como fallo y una validacion de negocio
 *       del proc no se podia distinguir de un error de conexion. Ahora se
 *       devuelve {@link RespuestaSp} con error, mensaje e id generado.</li>
 *   <li>Capturaba solo {@code BadSqlGrammarException} y se tragaba el resto
 *       imprimiendo con {@code System.out}; una violacion de clave foranea al
 *       eliminar pasaba de largo y la operacion se informaba como exitosa.</li>
 *   <li>Mandaba {@code ps.setInt} sobre columnas {@code bigint}
 *       ({@code idProveedorSap}, {@code audUsuario}) y {@code ps.setInt} sobre
 *       {@code codProvExtSap}, que es <b>varchar(20)</b>.</li>
 *   <li>Nunca enviaba {@code audFecha} y no tenia ningun metodo de consulta: el
 *       catalogo se leia desde otros procs.</li>
 * </ol>
 *
 * <p><b>Por que los listados usan el overload de Map y no el de modelo:</b>
 * {@code p_list_proveedorExtSap} trata {@code @parametro IS NULL} como "sin
 * filtro". El overload de modelo de {@code ejecutarListado} conserva los
 * {@code Number} en 0, de modo que un modelo recien construido podria filtrar
 * por {@code idProveedorSap = 0} y devolver cero filas. Con el Map se manda
 * unicamente lo que el que llama pidio filtrar.
 */
@Repository
public class ProveedorExtSapDao implements IProveedorExtSap {

    /** Procedimiento de altas, bajas y modificaciones. */
    private static final String SP_ABM  = "p_abm_proveedorExtSap";

    /** Procedimiento de consultas. */
    private static final String SP_LIST = "p_list_proveedorExtSap";

    private final SpHelper spHelper;

    public ProveedorExtSapDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Invoca {@code p_abm_proveedorExtSap} con ACCION "I", "U" o "D".
     *
     * <p>Se usa el overload de modelo porque los cinco campos de
     * {@link ProveedorExtSap} existen uno a uno como parametros del proc. En el
     * alta el proc graba {@code audFecha = GETDATE()} ignorando lo que se envie
     * y devuelve el id nuevo en {@code idGenerado}.
     *
     * @return respuesta del proc; lanza {@code SpBusinessException} si el proc
     *         reporta un error de negocio (por ejemplo, borrar un proveedor que
     *         ya esta asignado a productos)
     */
    @Override
    public RespuestaSp registrarProveedorExtSap(ProveedorExtSap mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    /**
     * Invoca {@code p_list_proveedorExtSap} ACCION "L" sin ningun filtro.
     *
     * @return todos los proveedores externos SAP
     */
    @Override
    public List<ProveedorExtSap> listarProveedores() {
        return listarProveedores(null, null, null, null);
    }

    /**
     * Invoca {@code p_list_proveedorExtSap} ACCION "L" enviando solo los
     * filtros no nulos; los que no se mandan quedan en el DEFAULT NULL del
     * proc, que es su forma de decir "sin filtro".
     *
     * <p>La rama "L" devuelve las cinco columnas de la tabla, asi que el
     * resultset mapea completo contra {@link ProveedorExtSap} y no hace falta
     * ningun DTO.
     *
     * @return los proveedores que cumplen los filtros
     */
    @Override
    public List<ProveedorExtSap> listarProveedores(Long idProveedorSap, String codProvExtSap,
                                                   String proveedorExtSap, Long audUsuario) {
        // LinkedHashMap: orden estable de los parametros en el EXEC generado.
        Map<String, Object> filtro = new LinkedHashMap<>();
        ponerSiHay(filtro, "idProveedorSap",  idProveedorSap);
        ponerSiHay(filtro, "codProvExtSap",   codProvExtSap);
        ponerSiHay(filtro, "proveedorExtSap", proveedorExtSap);
        ponerSiHay(filtro, "audUsuario",      audUsuario);

        return spHelper.ejecutarListado(SP_LIST, filtro, "L", ProveedorExtSap.class);
    }

    /**
     * Invoca {@code p_list_proveedorExtSap} ACCION "L" filtrando por
     * {@code @idProveedorSap}.
     *
     * @return la fila encontrada, o null si el id no existe
     */
    @Override
    public ProveedorExtSap obtenerPorId(long idProveedorSap) {
        List<ProveedorExtSap> filas = listarProveedores(idProveedorSap, null, null, null);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** Agrega el parametro solo si trae valor: null significa "sin filtro" en el proc. */
    private static void ponerSiHay(Map<String, Object> destino, String nombre, Object valor) {
        if (valor != null) {
            destino.put(nombre, valor);
        }
    }
}
