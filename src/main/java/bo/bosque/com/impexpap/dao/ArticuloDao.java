package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.ArticuloDisponibleDto;
import bo.bosque.com.impexpap.model.Articulo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de <b>tpr_articulo</b> a traves de {@code p_abm_articulo} y
 * {@code p_list_Articulo}. Todo pasa por {@code SpHelper}; no queda jdbcTemplate crudo.
 *
 * <p><b>Por que todos los listados arman un Map a mano y no mandan el model.</b>
 * {@code p_list_Articulo} usa {@code @param IS NULL} como "sin filtro" en practicamente
 * todas sus ramas, y el overload de modelo de {@code SpHelper.ejecutarListado} conserva
 * los Number en 0 (lo dice su propio javadoc). Mandar el model completo enviaria
 * stock=0, utm=0 y codigoFamilia=0 como filtros reales y el listado volveria vacio. El
 * Map lleva exclusivamente los parametros que la rama necesita; el resto se queda en el
 * DEFAULT NULL del procedimiento. La unica excepcion intencional es
 * {@code @idGrpFamiliaSap} de la rama 'D', donde el 0 SI es un valor con significado
 * ("todos los grupos") y por eso se envia siempre.
 *
 * <p><b>Riesgo heredado.</b> {@code p_list_Articulo} ejecuta
 * {@code p_abm_precioArtSAP @ACCION='A'} antes de evaluar la ACCION, de modo que
 * cualquier lectura de este DAO dispara una escritura de precios SAP. Ver el encabezado
 * de {@code resources/sql/tpr_Articulo.sql}.
 */
@Repository
public class ArticuloDao implements IArticulo {

    private static final String SP_ABM  = "p_abm_articulo";
    private static final String SP_LIST = "p_list_Articulo";

    private final SpHelper spHelper;

    public ArticuloDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Invoca {@code p_abm_articulo} con la ACCION recibida ('I', 'U' o 'D').
     *
     * <p>Se usa {@code ejecutarAbm} con el model porque el procedimiento (una vez aplicado
     * el script tpr_Articulo.sql) declara un parametro por cada columna de la tabla, que es
     * exactamente lo que el model contiene.
     *
     * @return RespuestaSp; idGenerado llega en 0 porque codArticulo no es IDENTITY
     */
    @Override
    public RespuestaSp registrarArticulo(Articulo articulo, String acc) {
        return this.spHelper.ejecutarAbm(SP_ABM, articulo, acc);
    }

    /**
     * ACCION 'L': devuelve las filas de tpr_articulo filtrando por codigo exacto.
     *
     * @param codArticulo null o vacio devuelve el listado completo
     * @return lista de Articulo; unidadMedida y gramajeSap llegan null porque la rama no los selecciona
     */
    @Override
    public List<Articulo> obtenerArticulos(String codArticulo) {
        Map<String, Object> filtro = new HashMap<>();
        if (codArticulo != null && !codArticulo.trim().isEmpty()) {
            filtro.put("codArticulo", codArticulo);
        }
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "L", Articulo.class);
    }

    /**
     * ACCION 'A': busqueda parcial (LIKE) por descripcion, ordenada por datoArt.
     *
     * <p>El parametro de familia se llama {@code @codProd} en el procedimiento, no
     * {@code @codigoFamilia}: la firma quedo con el nombre antiguo y hay que respetarla.
     *
     * @return lista de Articulo sin auditoria (la rama devuelve solo 6 columnas)
     */
    @Override
    public List<Articulo> buscarArticulos(Integer codigoFamilia, String datoArt, String datoArtExt) {
        Map<String, Object> filtro = new HashMap<>();
        if (codigoFamilia != null) {
            filtro.put("codProd", codigoFamilia);
        }
        if (datoArt != null && !datoArt.trim().isEmpty()) {
            filtro.put("datoArt", datoArt);
        }
        if (datoArtExt != null && !datoArtExt.trim().isEmpty()) {
            filtro.put("datoArtExt", datoArtExt);
        }
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "A", Articulo.class);
    }

    /**
     * ACCION 'B': catalogo codigo/descripcion para el buscador de la vista.
     *
     * <p>Es un UNION ALL entre tpr_articulo y tinv_Producto y no admite filtros, por eso
     * se manda un Map vacio. Solo vienen llenos codArticulo y datoArt.
     */
    @Override
    public List<Articulo> obtenerArticulosParaBusqueda() {
        return this.spHelper.ejecutarListado(SP_LIST, new HashMap<String, Object>(), "B", Articulo.class);
    }

    /**
     * ACCION 'D': articulos con disponibilidad y precio para el lado cliente.
     *
     * <p>{@code idGrpFamiliaSap} se envia siempre, incluso en 0, porque esta rama compara
     * {@code (@idGrpFamiliaSap = 0 OR @idGrpFamiliaSap = t1.idGrpFamiliaSap)}: el 0 es el
     * comodin "todos los grupos" y mandarlo en NULL dejaria la condicion en desconocido y
     * no devolveria ninguna fila.
     *
     * @return articulos con disponible mayor a 0 y precio mayor a 0 en esa lista y ciudad
     */
    @Override
    public List<ArticuloDisponibleDto> obtenerDisponiblesPorGrupoFamilia(int idGrpFamiliaSap, int listaPrecio, int codCiudad) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idGrpFamiliaSap", idGrpFamiliaSap);
        filtro.put("listaPrecio", listaPrecio);
        filtro.put("codCiudad", codCiudad);
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "D", ArticuloDisponibleDto.class);
    }

    /**
     * ACCION 'E': articulos con disponibilidad en una ciudad, sin informacion de precio.
     *
     * <p>La rama ignora {@code @listaPrecio} y {@code @idGrpFamiliaSap}, asi que no se envian.
     *
     * @return articulos con disponible mayor a 1; precio y listaPrecio llegan en 0 y moneda en null
     */
    @Override
    public List<ArticuloDisponibleDto> obtenerDisponiblesPorCiudad(int codCiudad) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codCiudad", codCiudad);
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "E", ArticuloDisponibleDto.class);
    }

    /**
     * ACCION 'F': todas las listas de precio de un articulo en una ciudad.
     *
     * <p>Los dos filtros son obligatorios: la rama los compara por igualdad sin el patron
     * {@code IS NULL OR}, de modo que un null aqui devuelve cero filas.
     *
     * @return una fila por lista de precio; datoArt, gramajeSap y unidadMedida llegan null
     *         y codCiudad en 0 porque el procedimiento devuelve literales en esas posiciones
     */
    @Override
    public List<ArticuloDisponibleDto> obtenerPreciosPorArticulo(String codArticulo, int codCiudad) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codArticulo", codArticulo);
        filtro.put("codCiudad", codCiudad);
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "F", ArticuloDisponibleDto.class);
    }

    /**
     * ACCION 'G': precios y disponibilidad de los articulos de una ciudad, lado vendedor.
     *
     * @return articulos con disponible mayor o igual a 1 y precio mayor a 0, con idGrpFamiliaSap
     */
    @Override
    public List<ArticuloDisponibleDto> obtenerPreciosVendedor(int codCiudad) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codCiudad", codCiudad);
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "G", ArticuloDisponibleDto.class);
    }

    /**
     * ACCION 'H': precios de un articulo para vendedores externos, sin la lista minima.
     *
     * <p>El rango de listas habilitadas sale de un CASE sobre la ciudad (1, 4 y 5). Para
     * cualquier otra ciudad el CASE devuelve NULL y la consulta no trae filas; es el
     * comportamiento del legacy y se conserva tal cual.
     */
    @Override
    public List<ArticuloDisponibleDto> obtenerPreciosVendedorExterno(String codArticulo, int codCiudad) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codArticulo", codArticulo);
        filtro.put("codCiudad", codCiudad);
        return this.spHelper.ejecutarListado(SP_LIST, filtro, "H", ArticuloDisponibleDto.class);
    }
}
