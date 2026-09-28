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
import bo.bosque.com.impexpap.model.Producto;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Contrato de acceso a datos de la tabla <b>tpr_producto</b> (familias de producto
 * del modulo de Precios). Un metodo por ACCION real de {@code p_abm_producto}
 * (I, U, D, E, F, G, H) y de {@code p_list_producto}
 * (LL, L, A, B, C, D, E, F, G, H, I, J, K, M, N, O, P, Q, R, S, T).
 *
 * <p>Todo el acceso pasa por procedimientos almacenados via {@code SpHelper};
 * no hay SQL crudo en ninguna implementacion.
 */
public interface IProducto {

    // =====================================================================
    // ABM  ->  p_abm_producto
    // =====================================================================

    /**
     * ACCION 'I': da de alta la familia y, en el mismo SP, le crea un precio en 0
     * por cada lista de precios activa de tpr_clasificacionPrecio.
     * <p>El SP fuerza estado=1, costoTM=0, idPropuestaAprobada=null y audFecha=GETDATE(),
     * sin importar lo que traiga el modelo.
     *
     * @param producto familia a registrar; codigoFamilia es obligatorio porque la PK NO es identity
     * @return respuesta del SP; {@code getIdGenerado()} devuelve el propio codigoFamilia
     */
    RespuestaSp registrar(Producto producto);

    /**
     * ACCION 'U': actualiza la familia identificada por codigoFamilia.
     * <p>El SP no actualiza costoTM ni idPropuestaAprobada: esos los mueve el flujo
     * de propuestas, no el mantenimiento de la familia.
     */
    RespuestaSp actualizar(Producto producto);

    /**
     * ACCION 'D': elimina fisicamente la familia de tpr_producto.
     * <p>No borra en cascada sus precios de tpr_precio (ver riesgos del ALTER).
     */
    RespuestaSp eliminar(int codigoFamilia, Long audUsuario);

    /** ACCION 'E': activa (estado=1) o desactiva (estado=0) una familia. */
    RespuestaSp cambiarEstado(int codigoFamilia, int estado, Long audUsuario);

    /** ACCION 'F': asigna el grupo de familia SAP a un codigo de familia. */
    RespuestaSp asignarGrupoFamiliaSap(int codigoFamilia, Long idGrpFamiliaSap, Long audUsuario);

    /** ACCION 'G': asigna el proveedor SAP a un codigo de familia. */
    RespuestaSp asignarProveedorSap(int codigoFamilia, Long idProveedorSap, Long audUsuario);

    /**
     * ACCION 'H': sincroniza los catalogos locales tpr_proveedorExtSap y
     * tpr_grupoFamiliaSap contra las vistas del SAP (v_SAP_ProveedoresFam y
     * v_SAP_Familias). No recibe codigo de familia: procesa todo el catalogo.
     */
    RespuestaSp sincronizarCatalogosSap(Long audUsuario);

    // =====================================================================
    // LISTADOS  ->  p_list_producto
    // =====================================================================

    /**
     * ACCION 'LL': registros crudos de tpr_producto (sin JOINs), filtrados por los
     * campos no nulos del filtro. Es el listado que alimenta la edicion.
     *
     * @param filtro campos en null = sin filtro; pasar {@code null} trae todo
     */
    List<Producto> listarCrudo(Producto filtro);

    /**
     * ACCION 'L': familias con sus descripciones resueltas (proveedor, grupo,
     * presentacion, tipo, rango y color), filtradas por los campos no nulos del filtro.
     */
    List<ProductoDto> listarConDescripcion(Producto filtro);

    /** ACCION 'L' filtrando por un codigo de familia; null si no existe. */
    ProductoDto obtenerConDescripcion(int codigoFamilia);

    /** ACCION 'A': precios vigentes de todas las familias activas. */
    List<ProductoPrecioDto> listarPreciosVigentes();

    /**
     * ACCION 'B': el mayor codigo de familia registrado.
     * <p>Sirve de sugerencia para el alta, porque la PK no es identity.
     *
     * @return el ultimo codigo, o null si la tabla esta vacia
     */
    Integer obtenerUltimoCodigoFamilia();

    /**
     * ACCION 'C': grupos de familia SAP que tienen productos activos.
     *
     * @param codigoFamilia codigo puntual; -1 o 0 significan "todos" para el SP
     */
    List<ProductoGrupoFamiliaDto> listarGruposFamiliaDeProductos(int codigoFamilia);

    /** ACCION 'D': familias activas de un grupo de familia SAP, con su proveedor. */
    List<ProductoProveedorDto> listarFamiliasPorGrupoFamilia(Long idGrpFamiliaSap);

    /** ACCION 'E': todas las familias activas que tienen proveedor SAP asignado. */
    List<ProductoProveedorDto> listarFamiliasConProveedor();

    /** ACCION 'F': valida si el codigo de familia ya existe antes de un alta. */
    boolean existeCodigoFamilia(int codigoFamilia);

    /** ACCION 'G': catalogo de proveedores SAP ordenado por nombre. */
    List<ProductoProveedorSapDto> listarProveedoresSap();

    /** ACCION 'H': catalogo de grupos de familia SAP ordenado por nombre. */
    List<ProductoGrupoFamiliaDto> listarGruposFamiliaSap();

    /**
     * ACCION 'I': PIVOT de los precios vigentes de un grupo de familia, con una
     * columna por lista de precio.
     * <p>Las columnas del PIVOT se llaman "1".."12" -las lee asi RptPrecioActFam del
     * JSF- y un nombre numerico no mapea a ninguna propiedad: el DAO lee la rama como
     * mapa y arma el DTO.
     */
    List<PrecioTonFamiliaDto> listarPreciosPivotPorGrupoFamilia(Long idGrpFamiliaSap);

    /**
     * ACCION 'J': comparativa de articulos SAP contra Bosque para un lote de familias.
     *
     * @param codCad lista de codigos separados por coma <b>con coma final</b>
     *               (ej. "1001,1002,"): el SP le quita el ultimo caracter
     */
    List<ProductoArticuloSapDto> compararArticulosSapBosque(String codCad);

    /**
     * ACCION 'K': PIVOT de los precios vigentes de TODAS las familias activas.
     * <p>Mismo tratamiento que {@link #listarPreciosPivotPorGrupoFamilia(Long)}; la rama
     * no trae color.
     */
    List<PrecioTonFamiliaDto> listarPreciosPivotTodos();

    /**
     * ACCION 'M': articulos que estan en el SAP y no en Bosque, para el mismo lote
     * de familias que la ACCION 'J'.
     * <p>Rama que en la base actual esta rota (usa columnas inexistentes); el script
     * {@code tpr_Producto.sql} la corrige. Sin ese ALTER este metodo lanza error 207.
     *
     * @param codCad lista de codigos separados por coma con coma final
     */
    List<ProductoArticuloSapDto> listarArticulosSoloEnSap(String codCad);

    /**
     * ACCION 'N': historial de costos sugeridos de una familia, solo de propuestas
     * aprobadas, desde {@code fechaI} hasta hoy.
     */
    List<ProductoCostoSugDto> listarHistorialCostoSugerido(int codigoFamilia, Date fechaI);

    /**
     * ACCION 'O': costo sugerido de una familia dentro de una propuesta.
     *
     * @return el costo, o null si esa propuesta no tiene costo para esa familia
     */
    BigDecimal obtenerCostoSugerido(Long idPropuesta, int codigoFamilia);

    /** ACCION 'P': familias activas con todas sus descripciones, para exportar. */
    List<ProductoExportDto> listarParaExportar();

    /**
     * ACCION 'Q': catalogo completo de grupos de familia SAP, sin ordenar.
     * <p>Misma forma que {@link #listarGruposFamiliaSap()} (ACCION 'H'), que ademas
     * ordena por nombre; se conservan las dos porque el JSF llama a las dos.
     */
    List<ProductoGrupoFamiliaDto> listarGruposFamiliaSapSinOrden();

    /**
     * ACCION 'R': catalogo completo de proveedores SAP, sin ordenar.
     * <p>Misma forma que {@link #listarProveedoresSap()} (ACCION 'G'), que ademas
     * ordena por nombre.
     */
    List<ProductoProveedorSapDto> listarProveedoresSapSinOrden();

    /**
     * ACCION 'S': tipo de cambio vigente leido del sistema IMPEXPAP/SOFI.
     * <p>Depende de un OPENROWSET al servidor 192.168.3.114: si ese enlace esta
     * caido, el SP falla.
     *
     * @return el tipo de cambio, o null si el origen no devolvio nada
     */
    BigDecimal obtenerTipoCambio();

    /**
     * ACCION 'T': grupos de familia con productos activos e idGrpFamiliaSap mayor a 2,
     * que es el listado que consume la pagina web publica.
     */
    List<ProductoGrupoFamiliaDto> listarGruposFamiliaWeb();

    // =====================================================================
    // HISTORIAL  ->  p_list_bitCostoProducto
    // =====================================================================

    /**
     * Historial del costo de una familia, la aprobacion mas reciente primero. Sale de
     * {@code tb_bitacora} con {@code p_list_bitCostoProducto}, el procedimiento del
     * dialogo "Bitacora de costo / propuesta" del JSF (no tiene @ACCION).
     */
    List<HistorialCostoFamiliaDto> listarHistorialCosto(int codigoFamilia);
}
