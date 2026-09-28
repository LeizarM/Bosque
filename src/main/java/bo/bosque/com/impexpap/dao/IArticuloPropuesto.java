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

import java.util.List;

/**
 * Contrato de acceso a <b>tpr_articuloPropuesto</b>. Un metodo por ACCION real de
 * {@code p_abm_ArticuloProp} y de {@code p_list_ArticuloProp}.
 *
 * <p>La ACCION 'E' del ABM no aparece: en la base su cuerpo esta integramente
 * comentado desde 2018 y ademas nombra columnas que ya no existen
 * (codProductoActual, stockProd, utmProd), asi que no se puede ni revivir tal cual.
 * Exponerla seria ofrecer una operacion que no hace nada.
 */
public interface IArticuloPropuesto {

    // ------------------------------------------------------------------ ABM ---

    /**
     * ABM generico: {@code p_abm_ArticuloProp} con ACCION 'I', 'U' o 'D'.
     * El model tiene exactamente las 9 columnas de la tabla y las 9 existen como
     * parametro del SP, asi que se manda entero.
     *
     * @param mb  articulo propuesto
     * @param acc 'I', 'U' o 'D'
     * @return {@link RespuestaSp}; en 'I' el idArticulo nuevo viaja en idGenerado
     */
    RespuestaSp registrarArticuloPropuesto(ArticuloPropuesto mb, String acc);

    /**
     * ACCION 'I'. Inserta el articulo en la propuesta.
     *
     * @return {@link RespuestaSp} con el idArticulo generado en idGenerado
     */
    RespuestaSp insertar(ArticuloPropuesto articuloPropuesto);

    /**
     * ACCION 'U'. Actualiza las ocho columnas no PK por idArticulo.
     */
    RespuestaSp actualizar(ArticuloPropuesto articuloPropuesto);

    /**
     * ACCION 'D'. Borra fisicamente la fila por idArticulo.
     */
    RespuestaSp eliminar(Long idArticulo);

    /**
     * ACCION 'B'. Solo cambia el codigoFamilia de un articulo ya propuesto.
     */
    RespuestaSp actualizarCodigoFamilia(Long idArticulo, Integer codigoFamilia);

    /**
     * ACCION 'C'. Carga masiva de los articulos de una propuesta.
     *
     * @param idPropuesta propuesta destino. En el legacy este parametro se recibia y
     *                    se descartaba (el proc tomaba siempre la ultima propuesta
     *                    creada); el script corregido lo respeta y solo cae en "la
     *                    ultima" cuando llega en null.
     * @param audUsuario  usuario de auditoria
     */
    RespuestaSp cargarArticulosDePropuesta(Long idPropuesta, Long audUsuario);

    /**
     * ACCION 'F'. Sincroniza tpr_articulo contra la vista v_SAP_Articulos: da de alta
     * los articulos nuevos y refresca stock, utm, descripcion y codigoFamilia de los
     * existentes. No escribe en tpr_articuloPropuesto.
     */
    RespuestaSp sincronizarArticulosSap(Long audUsuario);

    /**
     * ACCION 'G'. Agrega UN articulo a una propuesta copiandole familia, descripcion,
     * stock y utm desde tpr_articulo.
     */
    RespuestaSp agregarArticuloAPropuesta(Long idPropuesta, String codArticulo, Long audUsuario);

    // -------------------------------------------------------------- LISTADOS ---

    /**
     * ACCION 'L'. Listado plano de la tabla con filtros opcionales; los campos en null
     * del filtro no filtran.
     */
    List<ArticuloPropuesto> listar(ArticuloPropuesto filtro);

    /**
     * ACCION 'L' filtrando por PK.
     *
     * @return el articulo o null si no existe
     */
    ArticuloPropuesto obtenerPorId(Long idArticulo);

    /**
     * ACCION 'A'. Los articulos de una propuesta, solo las cinco columnas que necesita
     * la pantalla (codArticulo, datoArticulo, stock, utm, codigoFamilia).
     */
    List<ArticuloPropuesto> listarResumenPorPropuesta(Long idPropuesta);

    /**
     * ACCION 'B'. Los articulos de una propuesta con titulo y observacion de la
     * propuesta.
     */
    List<ArticuloPropuestoBDto> listarDetallePorPropuesta(Long idPropuesta);

    /**
     * ACCION 'C'. Articulos del catalogo (tpr_articulo) de una familia.
     *
     * <p>El nombre de la ACCION enganya: pese a llamarse "listar un articulo en base a
     * una propuesta", no filtra por propuesta — filtra por codigoFamilia.
     */
    List<ArticuloPropuestoCDto> listarArticulosPorFamilia(Integer codigoFamilia);

    /**
     * ACCION 'D'. Articulos afectados por la propuesta con los precios propuestos ya
     * calculados por unidad y convertidos a bolivianos.
     */
    List<ArticuloPropuestoDDto> listarPreciosPropuestos(Long idPropuesta);

    /**
     * ACCION 'E'. Articulos del catalogo de VARIAS familias.
     *
     * @param codigosFamilia lista de codigos separados por coma, con coma final
     *                       (ej. "12,13,14,"). Solo digitos, comas y espacios: el
     *                       proc la concatena dentro de un sp_executesql.
     */
    List<ArticuloPropuestoEDto> listarArticulosPorFamilias(String codigosFamilia);

    /**
     * ACCION 'F'. Comparativo de la propuesta pivoteado por lista de precio
     * (porcentajes, precio actual y precio propuesto en filas distintas).
     */
    List<ArticuloPropuestoFDto> listarComparativoPropuesta(Long idPropuesta);

    /**
     * ACCION 'G'. Grilla de articulos de la propuesta con el precio VIGENTE.
     */
    List<ArticuloPropuestoGDto> listarPreciosVigentes(Long idPropuesta);

    /**
     * ACCION 'H'. Reporte de actualizacion de precios por articulo, pivoteado por
     * lista de precio.
     */
    List<ArticuloPropuestoHDto> listarReportePreciosPorArticulo(Long idPropuesta);

    /**
     * ACCION 'I'. Familias del mismo grupo SAP y tipo que la propuesta que quedaron
     * fuera de ella.
     */
    List<ArticuloPropuestoIDto> listarFamiliasNoActualizadas(Long idPropuesta);
}
