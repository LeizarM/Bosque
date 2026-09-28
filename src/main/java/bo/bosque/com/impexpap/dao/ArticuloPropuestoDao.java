package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.ArticuloPropuestoBDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoCDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoDDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoEDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoFDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoGDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoHDto;
import bo.bosque.com.impexpap.dto.ArticuloPropuestoIDto;
import bo.bosque.com.impexpap.model.ArticuloPropuesto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos de <b>tpr_articuloPropuesto</b>. Todo pasa por los procedimientos
 * {@code p_abm_ArticuloProp} y {@code p_list_ArticuloProp} a traves de {@link SpHelper};
 * no queda una sola linea de SQL crudo.
 *
 * <p><b>Por que las escrituras parciales y todos los listados usan el overload de Map y
 * no el de modelo:</b> las diez ramas de {@code p_list_ArticuloProp} filtran con el
 * patron {@code (@x IS NULL OR @x = columna)}, o sea que NULL es el unico "sin filtro".
 * El overload de modelo de {@code SpHelper} conserva los {@code Number} en 0, asi que un
 * {@code codigoFamilia} o un {@code idPropuesta} en 0 se volveria un filtro real y
 * devolveria cero filas. Armando el Map a mano viaja unicamente lo que el llamador
 * quiere filtrar y el resto del SP queda en su DEFAULT NULL. El alta y la modificacion
 * completas si mandan el modelo: sus nueve campos existen los nueve como parametro del
 * SP.
 *
 * <p><b>Bugs del DAO legacy que NO se replican:</b>
 * <ul>
 *   <li>Se tragaba {@code BadSqlGrammarException}, lo imprimia por consola y devolvia
 *       {@code false} o una lista vacia, asi que un error de base pasaba por "no hay
 *       datos". Ahora {@code SpBusinessException} y los errores de base suben y los
 *       resuelve el GlobalExceptionHandler.</li>
 *   <li>Mapeaba los resultsets por indice de columna con lambdas: cualquier cambio de
 *       orden en el SELECT corrompia los datos en silencio. Ahora el mapeo es por
 *       NOMBRE de columna con BeanPropertyRowMapper, contra un DTO por forma de
 *       resultset.</li>
 *   <li>Mandaba las columnas al modelo con tipos primitivos ({@code setFloat},
 *       {@code setInt}) sobre columnas NULL-ables.</li>
 *   <li>Devolvia un {@code boolean} en el ABM y se perdia el id generado; ahora todas
 *       las escrituras devuelven {@link RespuestaSp}.</li>
 * </ul>
 *
 * <p><b>Bug del proc que se compensa desde aca:</b> la rama 'C' del ABM legacy pisa el
 * {@code @idPropuesta} recibido con la ultima propuesta creada
 * ({@code TOP(1) ... ORDER BY idPropuesta DESC}). El DAO manda siempre el id explicito
 * para que el script corregido lo respete y para que, el dia que se aplique el ALTER, no
 * haya que tocar Java.
 *
 * <p>La ACCION 'E' del ABM no se expone: su cuerpo esta integramente comentado en la
 * base y nombra columnas que ya no existen. Ver {@link IArticuloPropuesto}.
 */
@Repository
public class ArticuloPropuestoDao implements IArticuloPropuesto {

    /** Procedimiento de altas, bajas y modificaciones. */
    private static final String SP_ABM = "p_abm_ArticuloProp";

    /** Procedimiento de listados. */
    private static final String SP_LIST = "p_list_ArticuloProp";

    private final SpHelper spHelper;

    public ArticuloPropuestoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // ------------------------------------------------------------------ ABM ---

    /**
     * Punto de entrada generico del ABM: deriva a insertar, actualizar o eliminar segun
     * {@code acc}. Es lo que usa el controller, que decide 'I' o 'U' segun venga o no el
     * idArticulo.
     *
     * <p>La baja se deriva a {@link #eliminar(Long)} en vez de mandar el modelo entero:
     * la rama 'D' del SP solo mira {@code @idArticulo} y asi no viajan valores que mas
     * adelante alguien podria usar para pisar datos.
     *
     * @param mb  articulo propuesto
     * @param acc 'I', 'U' o 'D'
     * @return {@link RespuestaSp}; en el alta el idArticulo nuevo viene en idGenerado
     * @throws IllegalArgumentException si falta el modelo, la accion no es I/U/D o se
     *                                  pide borrar sin idArticulo
     */
    @Override
    public RespuestaSp registrarArticuloPropuesto(ArticuloPropuesto mb, String acc) {
        if (mb == null) throw new IllegalArgumentException("Falta el articulo propuesto.");
        if ("I".equalsIgnoreCase(acc)) return insertar(mb);
        if ("U".equalsIgnoreCase(acc)) return actualizar(mb);
        if ("D".equalsIgnoreCase(acc)) {
            Long id = mb.getIdArticulo();
            if (id == null) throw new IllegalArgumentException("Falta idArticulo para eliminar.");
            return eliminar(id);
        }
        throw new IllegalArgumentException("Accion no soportada para tpr_articuloPropuesto: " + acc);
    }

    /**
     * ACCION 'I' de {@code p_abm_ArticuloProp}: inserta la fila completa.
     *
     * <p>Se manda el modelo entero porque sus nueve campos existen los nueve como
     * parametro del SP. El idArticulo viaja en null (es IDENTITY) y el proc graba
     * audFecha con GETDATE(), ignorando lo que se le mande.
     *
     * @return {@link RespuestaSp} con el idArticulo nuevo en {@code getIdGenerado()}
     */
    @Override
    public RespuestaSp insertar(ArticuloPropuesto articuloPropuesto) {
        return spHelper.ejecutarAbm(SP_ABM, articuloPropuesto, "I");
    }

    /**
     * ACCION 'U': actualiza las ocho columnas no PK de la fila indicada por idArticulo.
     * Es un update total: lo que venga en null se graba en null.
     *
     * @return {@link RespuestaSp}; idGenerado queda en 0 porque no hay alta
     */
    @Override
    public RespuestaSp actualizar(ArticuloPropuesto articuloPropuesto) {
        return spHelper.ejecutarAbm(SP_ABM, articuloPropuesto, "U");
    }

    /**
     * ACCION 'D': borra fisicamente la fila. Solo viaja {@code @idArticulo}.
     */
    @Override
    public RespuestaSp eliminar(Long idArticulo) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idArticulo", idArticulo);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    /**
     * ACCION 'B': cambia unicamente el codigoFamilia del articulo ya propuesto. Por Map a
     * proposito, para no mandar en null el resto de las columnas de la fila.
     */
    @Override
    public RespuestaSp actualizarCodigoFamilia(Long idArticulo, Integer codigoFamilia) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idArticulo", idArticulo);
        params.put("codigoFamilia", codigoFamilia);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "B");
    }

    /**
     * ACCION 'C': carga masiva de los articulos de una propuesta.
     *
     * <p>Se manda {@code @idPropuesta} explicito. El proc publicado hoy lo descarta y se
     * queda con la ultima propuesta creada (bug del legacy); el script corregido lo
     * respeta y solo cae en "la ultima" cuando llega en null. Mandandolo, el
     * comportamiento es el correcto en cuanto se aplique el ALTER y no hay que volver a
     * tocar Java.
     */
    @Override
    public RespuestaSp cargarArticulosDePropuesta(Long idPropuesta, Long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "C");
    }

    /**
     * ACCION 'F': sincroniza tpr_articulo contra la vista v_SAP_Articulos (alta de los
     * articulos nuevos y refresco de stock, utm, descripcion y codigoFamilia de los que
     * ya estaban). No escribe en tpr_articuloPropuesto; el unico parametro que usa la
     * rama es {@code @audUsuario}.
     */
    @Override
    public RespuestaSp sincronizarArticulosSap(Long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "F");
    }

    /**
     * ACCION 'G': agrega UN articulo a la propuesta. La familia, la descripcion, el stock
     * y la utm no se mandan: el proc los copia de tpr_articulo buscando por codArticulo.
     */
    @Override
    public RespuestaSp agregarArticuloAPropuesta(Long idPropuesta, String codArticulo, Long audUsuario) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("codArticulo", codArticulo);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "G");
    }

    // -------------------------------------------------------------- LISTADOS ---

    /**
     * ACCION 'L': listado plano de tpr_articuloPropuesto. Solo viajan los campos no nulos
     * del filtro; con el filtro vacio (o en null) devuelve la tabla entera.
     */
    @Override
    public List<ArticuloPropuesto> listar(ArticuloPropuesto filtro) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (filtro != null) {
            ponerSiNoEsNulo(params, "idArticulo",    filtro.getIdArticulo());
            ponerSiNoEsNulo(params, "idPropuesta",   filtro.getIdPropuesta());
            ponerSiNoEsNulo(params, "codArticulo",   filtro.getCodArticulo());
            ponerSiNoEsNulo(params, "codigoFamilia", filtro.getCodigoFamilia());
            ponerSiNoEsNulo(params, "datoArticulo",  filtro.getDatoArticulo());
            ponerSiNoEsNulo(params, "stock",         filtro.getStock());
            ponerSiNoEsNulo(params, "utm",           filtro.getUtm());
            ponerSiNoEsNulo(params, "audUsuario",    filtro.getAudUsuario());
            ponerSiNoEsNulo(params, "audFecha",      filtro.getAudFecha());
        }
        return spHelper.ejecutarListado(SP_LIST, params, "L", ArticuloPropuesto.class);
    }

    /**
     * ACCION 'L' filtrando por PK.
     *
     * @return el articulo, o null si no existe
     */
    @Override
    public ArticuloPropuesto obtenerPorId(Long idArticulo) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idArticulo", idArticulo);
        List<ArticuloPropuesto> filas = spHelper.ejecutarListado(SP_LIST, params, "L", ArticuloPropuesto.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * ACCION 'A': los articulos de una propuesta con las cinco columnas que necesita la
     * pantalla. Se reusa el model porque las cinco (codArticulo, datoArticulo, stock, utm
     * y codigoFamilia) son columnas propias de la tabla; el resto de sus campos queda en
     * null porque la rama no los selecciona.
     */
    @Override
    public List<ArticuloPropuesto> listarResumenPorPropuesta(Long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, params, "A", ArticuloPropuesto.class);
    }

    /**
     * ACCION 'B': los articulos de la propuesta con el titulo y la observacion de
     * tpr_propuesta pegados por JOIN, por eso el DTO y no el model.
     */
    @Override
    public List<ArticuloPropuestoBDto> listarDetallePorPropuesta(Long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, params, "B", ArticuloPropuestoBDto.class);
    }

    /**
     * ACCION 'C': articulos del catalogo (tpr_articulo) de una familia.
     *
     * <p>Pese al comentario del proc ("listara un articulo en base a una propuesta") la
     * rama no mira {@code @idPropuesta}: filtra por {@code @codigoFamilia}, que es el
     * unico parametro que viaja.
     */
    @Override
    public List<ArticuloPropuestoCDto> listarArticulosPorFamilia(Integer codigoFamilia) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        return spHelper.ejecutarListado(SP_LIST, params, "C", ArticuloPropuestoCDto.class);
    }

    /**
     * ACCION 'D': articulos afectados por la propuesta con el precio propuesto ya bajado
     * a unidad y convertido a bolivianos.
     */
    @Override
    public List<ArticuloPropuestoDDto> listarPreciosPropuestos(Long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, params, "D", ArticuloPropuestoDDto.class);
    }

    /**
     * ACCION 'E': articulos del catalogo de VARIAS familias.
     *
     * <p>La rama concatena {@code @codCad} dentro de un {@code sp_executesql}, asi que el
     * parametro no es un parametro de verdad: es texto que termina siendo SQL. Por eso se
     * valida aca que traiga solo digitos, comas y espacios antes de mandarla; cualquier
     * otra cosa se rechaza en Java y nunca llega a la base.
     *
     * <p>Ademas el proc hace {@code SUBSTRING(@codCad, 1, LEN(@codCad)-1)} para sacar la
     * coma final: si la cadena no termina en coma, el proc se come el ultimo digito. Se
     * normaliza agregandola cuando falta, en vez de confiar en que el llamador la ponga.
     *
     * @param codigosFamilia por ejemplo "12,13,14," (la coma final es opcional aca)
     * @throws IllegalArgumentException si viene vacia o trae caracteres que no son
     *                                  digitos, comas o espacios
     */
    @Override
    public List<ArticuloPropuestoEDto> listarArticulosPorFamilias(String codigosFamilia) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codCad", normalizarListaDeFamilias(codigosFamilia));
        return spHelper.ejecutarListado(SP_LIST, params, "E", ArticuloPropuestoEDto.class);
    }

    /**
     * ACCION 'F': comparativo de la propuesta pivoteado por lista de precio. Es un solo
     * resultset: el UNION de los bloques de porcentajes, precio actual y precio
     * propuesto, que se distinguen por la columna {@code det}.
     *
     * <p>Se lee como mapa: las doce columnas se llaman "1".."12" (ver {@link FilaPivote}).
     */
    @Override
    public List<ArticuloPropuestoFDto> listarComparativoPropuesta(Long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("ACCION", "F");
        List<ArticuloPropuestoFDto> filas = new ArrayList<>();
        for (Map<String, Object> m : spHelper.ejecutarListadoDinamico(SP_LIST, params)) {
            ArticuloPropuestoFDto f = new ArticuloPropuestoFDto();
            f.setIdPropuesta(FilaPivote.largo(m, "idPropuesta"));
            f.setTitulo(FilaPivote.texto(m, "titulo"));
            f.setCodigoFamilia(FilaPivote.entero(m, "codigoFamilia"));
            f.setCodArticulo(FilaPivote.texto(m, "codArticulo"));
            f.setDatoArt(FilaPivote.texto(m, "datoArt"));
            f.setCostoTM(FilaPivote.decimal(m, "costoTM"));
            f.setDet(FilaPivote.texto(m, "det"));
            f.setUtm(FilaPivote.decimal(m, "utm"));
            for (int n = 1; n <= FilaPivote.LISTAS; n++) {
                f.ponerValorDeLista(n, FilaPivote.lista(m, n));
            }
            filas.add(f);
        }
        return filas;
    }

    /**
     * ACCION 'G': grilla de articulos de la propuesta con el precio VIGENTE
     * (tpr_precio.precio) en lugar del propuesto.
     */
    @Override
    public List<ArticuloPropuestoGDto> listarPreciosVigentes(Long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, params, "G", ArticuloPropuestoGDto.class);
    }

    /**
     * ACCION 'H': reporte de actualizacion de precios por articulo, pivoteado por lista de
     * precio. Como mapa, por el mismo motivo que la 'F'.
     */
    @Override
    public List<ArticuloPropuestoHDto> listarReportePreciosPorArticulo(Long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        params.put("ACCION", "H");
        List<ArticuloPropuestoHDto> filas = new ArrayList<>();
        for (Map<String, Object> m : spHelper.ejecutarListadoDinamico(SP_LIST, params)) {
            ArticuloPropuestoHDto f = new ArticuloPropuestoHDto();
            f.setIdPropuesta(FilaPivote.largo(m, "idPropuesta"));
            f.setObs(FilaPivote.texto(m, "obs"));
            f.setTitulo(FilaPivote.texto(m, "titulo"));
            f.setCodigoFamilia(FilaPivote.entero(m, "codigoFamilia"));
            f.setCostoTM(FilaPivote.decimal(m, "costoTM"));
            f.setCodArticulo(FilaPivote.texto(m, "codArticulo"));
            f.setDatoArticulo(FilaPivote.texto(m, "datoArticulo"));
            f.setUtm(FilaPivote.decimal(m, "utm"));
            for (int n = 1; n <= FilaPivote.LISTAS; n++) {
                f.ponerPrecioDeLista(n, FilaPivote.lista(m, n));
            }
            filas.add(f);
        }
        return filas;
    }

    /**
     * ACCION 'I': familias del mismo grupo SAP y tipo que la propuesta que quedaron fuera
     * de ella.
     *
     * <p>Es la rama mas pesada del proc: arma una tabla variable con un OPENROWSET al
     * servidor 192.168.3.114 y recien despues devuelve el resultset. Necesita
     * {@code SET NOCOUNT ON} en el proc (el script lo agrega) o el driver entrega el
     * update count del INSERT en vez de las filas.
     */
    @Override
    public List<ArticuloPropuestoIDto> listarFamiliasNoActualizadas(Long idPropuesta) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("idPropuesta", idPropuesta);
        return spHelper.ejecutarListado(SP_LIST, params, "I", ArticuloPropuestoIDto.class);
    }

    // ----------------------------------------------------------------- Apoyo ---

    /** Agrega el parametro solo si tiene valor: el SP trata NULL como "sin filtro". */
    private static void ponerSiNoEsNulo(Map<String, Object> params, String nombre, Object valor) {
        if (valor != null) {
            params.put(nombre, valor);
        }
    }

    /**
     * Deja la lista de familias como la espera la ACCION 'E': solo digitos y comas, sin
     * espacios y con una unica coma final. Lo que no sea digito, coma o espacio se
     * rechaza porque el proc concatena esta cadena dentro de un {@code sp_executesql}.
     */
    private static String normalizarListaDeFamilias(String codigosFamilia) {
        if (codigosFamilia == null || codigosFamilia.trim().isEmpty()) {
            throw new IllegalArgumentException("Falta la lista de codigos de familia.");
        }
        String limpio = codigosFamilia.replace(" ", "");
        if (!limpio.matches("[0-9]+(,[0-9]+)*,?")) {
            throw new IllegalArgumentException(
                    "La lista de codigos de familia solo admite digitos separados por coma: " + codigosFamilia);
        }
        return limpio.endsWith(",") ? limpio : limpio + ",";
    }
}
