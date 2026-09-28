package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.HistorialCostoFamiliaDto;
import bo.bosque.com.impexpap.dto.ProductoArticuloSapDto;
import bo.bosque.com.impexpap.dto.ProductoCostoSugDto;
import bo.bosque.com.impexpap.dto.ProductoDto;
import bo.bosque.com.impexpap.dto.PrecioTonFamiliaDto;
import bo.bosque.com.impexpap.dto.ProductoExportDto;
import bo.bosque.com.impexpap.dto.ProductoGrupoFamiliaDto;
import bo.bosque.com.impexpap.dto.ProductoPrecioDto;
import bo.bosque.com.impexpap.dto.ProductoProveedorDto;
import bo.bosque.com.impexpap.dto.ProductoProveedorSapDto;
import bo.bosque.com.impexpap.dto.ProductoTipoCambioDto;
import bo.bosque.com.impexpap.model.Producto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de la tabla <b>tpr_producto</b> (familias de producto del modulo
 * de Precios). Todo pasa por los procedimientos {@code p_abm_producto} y
 * {@code p_list_producto} a traves de {@link SpHelper}; no hay SQL crudo.
 *
 * <p><b>Por que todos los listados usan el overload de Map y nunca el de modelo:</b>
 * las 21 ramas de {@code p_list_producto} filtran con el patron
 * {@code (@x IS NULL OR @x = columna)}, es decir NULL es el unico "sin filtro".
 * El overload de modelo de {@code SpHelper} conserva los Number en 0, asi que un
 * campo numerico en 0 se convertiria en un filtro real y devolveria cero filas.
 * Construyendo el Map a mano solo viaja lo que el llamador realmente quiere filtrar.
 *
 * <p><b>Diferencias respecto del DAO legacy (bugs que NO se replican):</b>
 * <ul>
 *   <li>El legacy mapeaba por indice de columna con lambdas, asi que cualquier cambio
 *       de orden en el SELECT rompia los datos en silencio. Ahora el mapeo es por
 *       nombre de columna.</li>
 *   <li>El legacy se tragaba {@code BadSqlGrammarException} e imprimia por consola,
 *       devolviendo listas vacias como si la consulta hubiera ido bien. Ahora el error
 *       sube y lo maneja el GlobalExceptionHandler.</li>
 *   <li>El legacy mandaba {@code idRangoGramaje} a un parametro que se llama
 *       {@code @idRangoGram}, y el ABM viajaba con parametros posicionales.</li>
 *   <li>Las ramas I y K devuelven columnas de PIVOT llamadas "1".."12", que no son
 *       identificadores Java: se leen con {@code ejecutarListadoDinamico} en vez de
 *       inventar un DTO que quedaria vacio.</li>
 * </ul>
 */
@Repository
public class ProductoDao implements IProducto {

    private static final String SP_ABM  = "p_abm_producto";
    private static final String SP_LIST = "p_list_producto";
    private static final String SP_BIT_COSTO = "p_list_bitCostoProducto";

    private final SpHelper spHelper;

    public ProductoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // =====================================================================
    // ABM  ->  p_abm_producto
    // =====================================================================

    /**
     * ACCION 'I' de {@code p_abm_producto}: inserta la familia y le crea un precio en 0
     * por cada lista de precios activa.
     *
     * @return {@link RespuestaSp}; {@code idGenerado} trae el propio codigoFamilia
     *         porque la PK de tpr_producto NO es identity
     */
    @Override
    public RespuestaSp registrar(Producto producto) {
        return spHelper.ejecutarAbm(SP_ABM, producto, "I");
    }

    /**
     * ACCION 'U' de {@code p_abm_producto}: actualiza la familia por codigoFamilia.
     * costoTM e idPropuestaAprobada no se tocan en esta rama.
     */
    @Override
    public RespuestaSp actualizar(Producto producto) {
        return spHelper.ejecutarAbm(SP_ABM, producto, "U");
    }

    /**
     * ACCION 'D' de {@code p_abm_producto}: borra la familia.
     * Se usa el overload de Map para no mandar el resto de columnas en null.
     */
    @Override
    public RespuestaSp eliminar(int codigoFamilia, Long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    /** ACCION 'E' de {@code p_abm_producto}: cambia solo el estado de la familia. */
    @Override
    public RespuestaSp cambiarEstado(int codigoFamilia, int estado, Long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        params.put("estado", estado);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "E");
    }

    /** ACCION 'F' de {@code p_abm_producto}: asigna el grupo de familia SAP. */
    @Override
    public RespuestaSp asignarGrupoFamiliaSap(int codigoFamilia, Long idGrpFamiliaSap, Long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        params.put("idGrpFamiliaSap", idGrpFamiliaSap);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "F");
    }

    /** ACCION 'G' de {@code p_abm_producto}: asigna el proveedor SAP. */
    @Override
    public RespuestaSp asignarProveedorSap(int codigoFamilia, Long idProveedorSap, Long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        params.put("idProveedorSap", idProveedorSap);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "G");
    }

    /**
     * ACCION 'H' de {@code p_abm_producto}: sincroniza tpr_proveedorExtSap y
     * tpr_grupoFamiliaSap contra las vistas del SAP. Es una operacion masiva:
     * no recibe codigo de familia.
     */
    @Override
    public RespuestaSp sincronizarCatalogosSap(Long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "H");
    }

    // =====================================================================
    // LISTADOS  ->  p_list_producto
    // =====================================================================

    /**
     * ACCION 'LL': filas crudas de tpr_producto.
     *
     * @return lista de {@link Producto} (las 13 columnas del SELECT mas audFecha,
     *         que agrega el ALTER al final para completar el modelo)
     */
    @Override
    public List<Producto> listarCrudo(Producto filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtroComoMap(filtro), "LL", Producto.class);
    }

    /** ACCION 'L': familias con las descripciones resueltas por JOIN. */
    @Override
    public List<ProductoDto> listarConDescripcion(Producto filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtroComoMap(filtro), "L", ProductoDto.class);
    }

    /**
     * ACCION 'L' acotada a un codigo de familia.
     *
     * @return el primer registro, o null si el codigo no existe
     */
    @Override
    public ProductoDto obtenerConDescripcion(int codigoFamilia) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        List<ProductoDto> lista = spHelper.ejecutarListado(SP_LIST, params, "L", ProductoDto.class);
        return lista.isEmpty() ? null : lista.get(0);
    }

    /** ACCION 'A': precios vigentes de todas las familias activas. */
    @Override
    public List<ProductoPrecioDto> listarPreciosVigentes() {
        return spHelper.ejecutarListado(SP_LIST, sinFiltros(), "A", ProductoPrecioDto.class);
    }

    /**
     * ACCION 'B': mayor codigo de familia de la tabla.
     *
     * @return el ultimo codigo, o null si tpr_producto esta vacia
     */
    @Override
    public Integer obtenerUltimoCodigoFamilia() {
        List<Producto> lista = spHelper.ejecutarListado(SP_LIST, sinFiltros(), "B", Producto.class);
        return lista.isEmpty() ? null : lista.get(0).getCodigoFamilia();
    }

    /**
     * ACCION 'C': grupos de familia SAP con productos activos.
     *
     * @param codigoFamilia codigo puntual; -1 o 0 hacen que el SP devuelva todos
     */
    @Override
    public List<ProductoGrupoFamiliaDto> listarGruposFamiliaDeProductos(int codigoFamilia) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        return spHelper.ejecutarListado(SP_LIST, params, "C", ProductoGrupoFamiliaDto.class);
    }

    /** ACCION 'D': familias activas de un grupo de familia SAP, con su proveedor. */
    @Override
    public List<ProductoProveedorDto> listarFamiliasPorGrupoFamilia(Long idGrpFamiliaSap) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idGrpFamiliaSap", idGrpFamiliaSap);
        return spHelper.ejecutarListado(SP_LIST, params, "D", ProductoProveedorDto.class);
    }

    /**
     * ACCION 'E': familias activas con proveedor SAP asignado.
     * El campo grpFam del DTO queda null: esta rama no lo devuelve.
     */
    @Override
    public List<ProductoProveedorDto> listarFamiliasConProveedor() {
        return spHelper.ejecutarListado(SP_LIST, sinFiltros(), "E", ProductoProveedorDto.class);
    }

    /**
     * ACCION 'F': verifica duplicados de codigo de familia.
     *
     * @return true si el codigo ya existe en tpr_producto
     */
    @Override
    public boolean existeCodigoFamilia(int codigoFamilia) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        return !spHelper.ejecutarListado(SP_LIST, params, "F", Producto.class).isEmpty();
    }

    /** ACCION 'G': catalogo de proveedores SAP ordenado por nombre. */
    @Override
    public List<ProductoProveedorSapDto> listarProveedoresSap() {
        return spHelper.ejecutarListado(SP_LIST, sinFiltros(), "G", ProductoProveedorSapDto.class);
    }

    /** ACCION 'H': catalogo de grupos de familia SAP ordenado por nombre. */
    @Override
    public List<ProductoGrupoFamiliaDto> listarGruposFamiliaSap() {
        return spHelper.ejecutarListado(SP_LIST, sinFiltros(), "H", ProductoGrupoFamiliaDto.class);
    }

    /**
     * ACCION 'I': PIVOT de precios vigentes de un grupo de familia.
     * Se usa {@code ejecutarListadoDinamico} porque 12 de las 19 columnas se llaman
     * "1".."12" y no hay propiedad Java posible para ellas (ver {@link FilaPivote}).
     *
     * @return una fila por familia, con el precio de cada lista (null si esa familia no
     *         tiene precio en esa lista)
     */
    @Override
    public List<PrecioTonFamiliaDto> listarPreciosPivotPorGrupoFamilia(Long idGrpFamiliaSap) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idGrpFamiliaSap", idGrpFamiliaSap);
        params.put("ACCION", "I");
        List<PrecioTonFamiliaDto> filas = new ArrayList<>();
        for (Map<String, Object> m : spHelper.ejecutarListadoDinamico(SP_LIST, params)) {
            filas.add(precioTon(m, "grpFam", "proveedorExtSap", FilaPivote.texto(m, "color")));
        }
        return filas;
    }

    /**
     * ACCION 'J': comparativa de articulos entre el SAP y Bosque.
     *
     * @param codCad codigos separados por coma y con coma final ("1001,1002,"),
     *               porque el SP le corta el ultimo caracter antes de armar el IN
     */
    @Override
    public List<ProductoArticuloSapDto> compararArticulosSapBosque(String codCad) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codCad", codCad);
        return spHelper.ejecutarListado(SP_LIST, params, "J", ProductoArticuloSapDto.class);
    }

    /**
     * ACCION 'K': PIVOT de precios vigentes de todas las familias activas.
     * Mapas por el mismo motivo que la ACCION 'I'; la rama no trae color.
     */
    @Override
    public List<PrecioTonFamiliaDto> listarPreciosPivotTodos() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("ACCION", "K");
        List<PrecioTonFamiliaDto> filas = new ArrayList<>();
        for (Map<String, Object> m : spHelper.ejecutarListadoDinamico(SP_LIST, params)) {
            filas.add(precioTon(m, "grpFamilia", "proveedor", null));
        }
        return filas;
    }

    /** Las ramas I y K devuelven lo mismo con otros nombres de grupo y proveedor. */
    private static PrecioTonFamiliaDto precioTon(Map<String, Object> m, String claveGrupo,
                                                 String claveProveedor, String color) {
        PrecioTonFamiliaDto f = new PrecioTonFamiliaDto();
        f.setCodigoFamilia(FilaPivote.entero(m, "codigoFamilia"));
        f.setGrupo(FilaPivote.texto(m, claveGrupo));
        f.setProveedor(FilaPivote.texto(m, claveProveedor));
        f.setPresentacion(FilaPivote.texto(m, "presentacion"));
        f.setTipo(FilaPivote.texto(m, "tipo"));
        f.setColor(color);
        f.setCostoTM(FilaPivote.decimal(m, "costoTM"));
        for (int n = 1; n <= FilaPivote.LISTAS; n++) {
            f.ponerPrecioDeLista(n, FilaPivote.lista(m, n));
        }
        return f;
    }

    /**
     * ACCION 'M': articulos que existen en el SAP y no en Bosque.
     * Requiere el ALTER de {@code tpr_Producto.sql}: en la base actual la rama
     * referencia columnas inexistentes y siempre falla.
     */
    @Override
    public List<ProductoArticuloSapDto> listarArticulosSoloEnSap(String codCad) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codCad", codCad);
        return spHelper.ejecutarListado(SP_LIST, params, "M", ProductoArticuloSapDto.class);
    }

    /**
     * ACCION 'N': historial de costos sugeridos de una familia (solo propuestas
     * aprobadas) desde fechaI hasta hoy.
     *
     * @param fechaI limite inferior; el SP la compara con {@code >=}, asi que si
     *               llega null la rama no devuelve filas
     */
    @Override
    public List<ProductoCostoSugDto> listarHistorialCostoSugerido(int codigoFamilia, Date fechaI) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        params.put("fechaI", fechaI);
        return spHelper.ejecutarListado(SP_LIST, params, "N", ProductoCostoSugDto.class);
    }

    /**
     * ACCION 'O': costo sugerido de una familia dentro de una propuesta.
     *
     * @return el costo, o null si no hay registro para ese par propuesta/familia
     */
    @Override
    public BigDecimal obtenerCostoSugerido(Long idPropuesta, int codigoFamilia) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("codigoFamilia", codigoFamilia);
        List<ProductoCostoSugDto> lista = spHelper.ejecutarListado(SP_LIST, params, "O", ProductoCostoSugDto.class);
        return lista.isEmpty() ? null : lista.get(0).getCostoSug();
    }

    /** ACCION 'P': familias activas con todas sus descripciones, para exportar. */
    @Override
    public List<ProductoExportDto> listarParaExportar() {
        return spHelper.ejecutarListado(SP_LIST, sinFiltros(), "P", ProductoExportDto.class);
    }

    /** ACCION 'Q': grupos de familia SAP sin ordenar (equivalente a la ACCION 'H'). */
    @Override
    public List<ProductoGrupoFamiliaDto> listarGruposFamiliaSapSinOrden() {
        return spHelper.ejecutarListado(SP_LIST, sinFiltros(), "Q", ProductoGrupoFamiliaDto.class);
    }

    /** ACCION 'R': proveedores SAP sin ordenar (equivalente a la ACCION 'G'). */
    @Override
    public List<ProductoProveedorSapDto> listarProveedoresSapSinOrden() {
        return spHelper.ejecutarListado(SP_LIST, sinFiltros(), "R", ProductoProveedorSapDto.class);
    }

    /**
     * ACCION 'S': tipo de cambio vigente del sistema IMPEXPAP/SOFI.
     * Requiere el ALTER de {@code tpr_Producto.sql}, que le pone el alias {@code tc}
     * a la columna (el legacy hacia {@code Select @tc}, sin nombre de columna).
     *
     * @return el tipo de cambio, o null si el origen remoto no devolvio valor
     */
    @Override
    public BigDecimal obtenerTipoCambio() {
        List<ProductoTipoCambioDto> lista =
                spHelper.ejecutarListado(SP_LIST, sinFiltros(), "S", ProductoTipoCambioDto.class);
        return lista.isEmpty() ? null : lista.get(0).getTc();
    }

    /** ACCION 'T': grupos de familia activos para la pagina web. */
    @Override
    public List<ProductoGrupoFamiliaDto> listarGruposFamiliaWeb() {
        return spHelper.ejecutarListado(SP_LIST, sinFiltros(), "T", ProductoGrupoFamiliaDto.class);
    }

    // =====================================================================
    // Apoyo
    // =====================================================================

    /**
     * Arma el Map de filtros a partir del modelo, salteando los null.
     *
     * <p>No se usa {@code ejecutarListado(sp, modelo, ...)} justamente por esto: ese
     * overload conserva los Number en 0 y las ramas LL y L usan
     * {@code @param IS NULL} como unico "sin filtro", asi que un 0 filtraria de verdad.
     * Tampoco se mandan los campos que esas ramas no saben filtrar.
     */
    private Map<String, Object> filtroComoMap(Producto filtro) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (filtro == null) return params;

        ponerSiNoEsNulo(params, "codigoFamilia",       filtro.getCodigoFamilia());
        ponerSiNoEsNulo(params, "idGrpFamiliaSap",     filtro.getIdGrpFamiliaSap());
        ponerSiNoEsNulo(params, "idProveedorSap",      filtro.getIdProveedorSap());
        ponerSiNoEsNulo(params, "idPresentacion",      filtro.getIdPresentacion());
        ponerSiNoEsNulo(params, "idTipo",              filtro.getIdTipo());
        ponerSiNoEsNulo(params, "idRangoGram",         filtro.getIdRangoGram());
        ponerSiNoEsNulo(params, "formato",             filtro.getFormato());
        ponerSiNoEsNulo(params, "gramaje",             filtro.getGramaje());
        ponerSiNoEsNulo(params, "idColor",             filtro.getIdColor());
        ponerSiNoEsNulo(params, "estado",              filtro.getEstado());
        ponerSiNoEsNulo(params, "costoTM",             filtro.getCostoTM());
        ponerSiNoEsNulo(params, "idPropuestaAprobada", filtro.getIdPropuestaAprobada());
        ponerSiNoEsNulo(params, "audUsuario",          filtro.getAudUsuario());
        ponerSiNoEsNulo(params, "audFecha",            filtro.getAudFecha());

        return params;
    }

    // =====================================================================
    // HISTORIAL  ->  p_list_bitCostoProducto
    // =====================================================================

    /**
     * Se lee como mapa y se convierte aca: {@code tb_bitacora} guarda los valores como
     * texto (la propuesta y el costo anterior y nuevo), y BeanPropertyRowMapper no pasa un
     * varchar a Long ni a BigDecimal.
     */
    @Override
    public List<HistorialCostoFamiliaDto> listarHistorialCosto(int codigoFamilia) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        List<Map<String, Object>> filas = spHelper.ejecutarListadoDinamico(SP_BIT_COSTO, params);
        List<HistorialCostoFamiliaDto> lista = new ArrayList<>(filas.size());
        for (Map<String, Object> f : filas) {
            Long codigo = aLong(f.get("codigoFamilia"));
            Object fecha = f.get("fecha");
            Object usuario = f.get("usuario");
            lista.add(new HistorialCostoFamiliaDto(
                    codigo == null ? null : codigo.intValue(),
                    fecha instanceof Date ? (Date) fecha : null,
                    usuario == null ? "" : usuario.toString().trim(),
                    aLong(f.get("audUsuario")),
                    aLong(f.get("propuestaAnterior")),
                    aLong(f.get("propuestaNueva")),
                    aDecimal(f.get("costoAnterior")),
                    aDecimal(f.get("costoNuevo")),
                    aDecimal(f.get("costoActual")),
                    aLong(f.get("propuestaActual"))));
        }
        return lista;
    }

    /** Numero o texto numerico; cualquier otra cosa (null, vacio, "NULL") queda en null. */
    static BigDecimal aDecimal(Object valor) {
        if (valor == null) return null;
        if (valor instanceof BigDecimal) return (BigDecimal) valor;
        if (valor instanceof Number) return new BigDecimal(valor.toString());
        String t = valor.toString().trim();
        if (t.isEmpty()) return null;
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Long aLong(Object valor) {
        BigDecimal d = aDecimal(valor);
        if (d == null) return null;
        try {
            return d.longValueExact();
        } catch (ArithmeticException e) {
            return null;
        }
    }

    /** Map vacio: el SP recibe solo @ACCION y todos sus filtros quedan en el DEFAULT NULL. */
    private Map<String, Object> sinFiltros() {
        return new LinkedHashMap<String, Object>();
    }

    /** Agrega el parametro solo si tiene valor; los null no viajan para que el SP use su DEFAULT. */
    private void ponerSiNoEsNulo(Map<String, Object> params, String nombre, Object valor) {
        if (valor != null) params.put(nombre, valor);
    }
}
