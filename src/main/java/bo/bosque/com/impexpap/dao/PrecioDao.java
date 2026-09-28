package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.PrecioADto;
import bo.bosque.com.impexpap.dto.PrecioCDto;
import bo.bosque.com.impexpap.dto.PrecioDDto;
import bo.bosque.com.impexpap.dto.PrecioGDto;
import bo.bosque.com.impexpap.model.Precio;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a dbo.tpr_precio. Sin SQL crudo: todo por SpHelper contra
 * {@code p_abm_precio} y {@code p_list_precio}.
 *
 * <p><b>Por que se usa el overload de Map en todos los listados:</b> la rama L
 * de {@code p_list_precio} filtra con {@code (@x IS NULL OR @x = columna)}, o
 * sea que el "sin filtro" es NULL y un 0 seria un filtro real que devuelve cero
 * filas. El overload de modelo conserva los Number en 0 a proposito, asi que
 * cualquier parametro que no sea un filtro deliberado se arma a mano.
 *
 * <p><b>Rama F de p_list_precio: NO se expone.</b> Arma SQL dinamico sobre
 * {@code tpr_producto.codigo}, {@code tpr_producto.proveedor},
 * {@code tpr_producto.familia} y {@code tpr_precio.codigo}, cuatro columnas que
 * hoy no existen en la base. Cualquier llamada revienta con error 207
 * ("Invalid column name"). No es un bug de mapeo, es una rama muerta: quedo de
 * un esquema anterior donde la PK del producto era "codigo" y no
 * "codigoFamilia". Envolverla en un metodo Java solo trasladaria el error al
 * runtime. Si el negocio todavia necesita "listar precios de varias familias
 * mandando una cadena de codigos", hay que reescribir la rama contra el esquema
 * actual y recien ahi agregarla aca.
 *
 * <p>Bugs del legacy que este DAO NO replica:
 * <ul>
 *   <li>El viejo {@code registrarPrecio} devolvia {@code false} siempre: nunca
 *       llego a llamar al ABM.</li>
 *   <li>El viejo {@code listPrecioToneladasActuales} leia la rama D por indice
 *       de columna (1..19), asi que cualquier cambio en el SELECT desalineaba
 *       todos los campos en silencio. Ahora se mapea por nombre.</li>
 *   <li>Ese mismo metodo se tragaba la {@code BadSqlGrammarException} y
 *       devolvia lista vacia: un error de base se veia como "no hay datos".</li>
 * </ul>
 */
@Repository
public class PrecioDao implements IPrecio {

    /** Procedimiento de altas, bajas y modificaciones de tpr_precio. */
    private static final String SP_ABM  = "p_abm_precio";

    /** Procedimiento de listados de tpr_precio. */
    private static final String SP_LIST = "p_list_precio";

    private final SpHelper spHelper;

    public PrecioDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    /**
     * Invoca {@code p_abm_precio} con ACCION I, U o D.
     *
     * <p>Manda los 6 campos del modelo como parametros; el SP pisa audFecha con
     * GETDATE() tanto en el alta como en la modificacion.
     *
     * @return la respuesta del SP. En el alta, {@code getIdGenerado()} trae el
     *         idPrecio recien creado (SCOPE_IDENTITY).
     */
    @Override
    public RespuestaSp registrar(Precio precio, String accion) {
        return spHelper.ejecutarAbmMap(SP_ABM, parametros(precio), accion);
    }

    /**
     * Los campos del model como parametros de {@code p_abm_precio}.
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
    static Map<String, Object> parametros(Precio precio) {
        Map<String, Object> p = new java.util.LinkedHashMap<>();
        if (precio.getIdPrecio() != null) p.put("idPrecio", precio.getIdPrecio());
        if (precio.getCodigoFamilia() != null) p.put("codigoFamilia", precio.getCodigoFamilia());
        if (precio.getIdClasificacion() != null) p.put("idClasificacion", precio.getIdClasificacion());
        if (precio.getPrecio() != null) p.put("precio", precio.getPrecio());
        if (precio.getAudUsuario() != null) p.put("audUsuario", precio.getAudUsuario());
        return p;
    }

    /**
     * Invoca {@code p_list_precio} con ACCION L y devuelve filas de tpr_precio.
     *
     * <p>Se arma el Map a mano con los campos no nulos del filtro: son los
     * unicos cinco que la rama L usa en el WHERE. audFecha no filtra nada en
     * esa rama, asi que no se manda.
     */
    @Override
    public List<Precio> listar(Precio filtro) {
        Map<String, Object> params = new HashMap<>();
        if (filtro != null) {
            if (filtro.getIdPrecio()        != null) params.put("idPrecio",        filtro.getIdPrecio());
            if (filtro.getCodigoFamilia()   != null) params.put("codigoFamilia",   filtro.getCodigoFamilia());
            if (filtro.getIdClasificacion() != null) params.put("idClasificacion", filtro.getIdClasificacion());
            if (filtro.getPrecio()          != null) params.put("precio",          filtro.getPrecio());
            if (filtro.getAudUsuario()      != null) params.put("audUsuario",      filtro.getAudUsuario());
        }
        return spHelper.ejecutarListado(SP_LIST, params, "L", Precio.class);
    }

    /**
     * Invoca {@code p_list_precio} con ACCION L filtrando solo por la PK.
     *
     * @return el registro, o null si no existe
     */
    @Override
    public Precio obtenerPorId(long idPrecio) {
        Map<String, Object> params = new HashMap<>();
        params.put("idPrecio", idPrecio);

        List<Precio> lst = spHelper.ejecutarListado(SP_LIST, params, "L", Precio.class);
        return lst.isEmpty() ? null : lst.get(0);
    }

    /**
     * Invoca {@code p_list_precio} con ACCION A: ficha de la familia con su
     * precio y su clasificacion, para llenar el formulario de captura.
     *
     * @return una fila por precio de la familia, con proveedor y grupo de
     *         familia ya resueltos a texto por el SP
     */
    @Override
    public List<PrecioADto> obtenerDatosCaptura(int codigoFamilia) {
        Map<String, Object> params = new HashMap<>();
        params.put("codigoFamilia", codigoFamilia);

        return spHelper.ejecutarListado(SP_LIST, params, "A", PrecioADto.class);
    }

    /**
     * Invoca {@code p_list_precio} con ACCION C: los precios de una familia,
     * uno por clasificacion y sucursal, en version corta (7 columnas).
     */
    @Override
    public List<PrecioCDto> listarPorFamilia(int codigoFamilia) {
        Map<String, Object> params = new HashMap<>();
        params.put("codigoFamilia", codigoFamilia);

        return spHelper.ejecutarListado(SP_LIST, params, "C", PrecioCDto.class);
    }

    /**
     * Invoca {@code p_list_precio} con ACCION D: la grilla completa de reprecio
     * de una familia. Solo trae clasificaciones con estado = 1.
     *
     * @return una fila por precio, con producto, sucursal, lista, porcentaje,
     *         iva e it; ordenadas por vpp
     */
    @Override
    public List<PrecioDDto> listarDetalleParaReprecio(int codigoFamilia) {
        Map<String, Object> params = new HashMap<>();
        params.put("codigoFamilia", codigoFamilia);

        return spHelper.ejecutarListado(SP_LIST, params, "D", PrecioDDto.class);
    }

    /**
     * Invoca {@code p_list_precio} con ACCION E: igual forma que la rama D,
     * pero excluyendo los precios de la familia que ya estan cargados en la
     * propuesta indicada. Sirve para no agregar dos veces el mismo precio.
     *
     * <p>Ojo: a diferencia de D, esta rama usa INNER JOIN contra
     * tpr_grupoFamiliaSap y tpr_proveedorExtSap, asi que un producto sin grupo
     * de familia o sin proveedor SAP asignado no aparece. Es el comportamiento
     * historico y se conserva.
     */
    @Override
    public List<PrecioDDto> listarNoIncluidosEnPropuesta(int codigoFamilia, long idPropuesta) {
        Map<String, Object> params = new HashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        params.put("idPropuesta",   idPropuesta);

        return spHelper.ejecutarListado(SP_LIST, params, "E", PrecioDDto.class);
    }

    /**
     * Invoca {@code p_list_precio} con ACCION G: los precios de una familia
     * dentro de una propuesta, con el valor actual y el propuesto.
     *
     * @return filas ordenadas por vpp y sucursal
     */
    @Override
    public List<PrecioGDto> listarDePropuesta(int codigoFamilia, long idPropuesta) {
        Map<String, Object> params = new HashMap<>();
        params.put("codigoFamilia", codigoFamilia);
        params.put("idPropuesta",   idPropuesta);

        return spHelper.ejecutarListado(SP_LIST, params, "G", PrecioGDto.class);
    }
}
