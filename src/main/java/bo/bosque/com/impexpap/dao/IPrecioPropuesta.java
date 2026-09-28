package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.PrecioPropuestaADto;
import bo.bosque.com.impexpap.dto.PrecioPropuestaBDto;
import bo.bosque.com.impexpap.dto.PrecioPropuestaDDto;
import bo.bosque.com.impexpap.model.PrecioPropuesta;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.math.BigDecimal;
import java.util.List;

/**
 * Operaciones sobre tpr_precioPropuesta. Un metodo por ACCION real de
 * p_abm_precioPropuesta (I, U, D, B, C, E) y de p_list_precioPropuesta (L, A, B, C, D).
 *
 * <p>Requiere el ALTER de {@code src/main/resources/sql/tpr_PrecioPropuesta.sql}:
 * sin el, el ABM no recibe el id (el parametro legacy se llama {@code @idPrePropuesto})
 * y no hay parametros de salida {@code @error/@errormsg/@idGenerado}.
 */
public interface IPrecioPropuesta {

    // ─────────────────────────── ABM (p_abm_precioPropuesta) ───────────────────────────

    /**
     * Punto de entrada unico para el alta/baja/modificacion: delega segun la accion.
     *
     * @param mb  fila de tpr_precioPropuesta
     * @param acc "I" (insertar), "U" (actualizar) o "D" (eliminar)
     */
    RespuestaSp registrarPrecioPropuesta(PrecioPropuesta mb, String acc);

    /** ACCION 'I': inserta la fila. Devuelve el idPrecioPropuesto nuevo en {@code idGenerado}. */
    RespuestaSp insertar(PrecioPropuesta mb);

    /** ACCION 'U': actualiza todas las columnas de la fila indicada por idPrecioPropuesto. */
    RespuestaSp actualizar(PrecioPropuesta mb);

    /** ACCION 'D': elimina la fila por id. */
    RespuestaSp eliminar(long idPrecioPropuesto);

    /**
     * ACCION 'B': actualiza SOLO el precio propuesto de una fila (edicion en linea de la
     * grilla). No toca porcentaje, ni auditoria, ni el resto de columnas.
     */
    RespuestaSp actualizarPrecioPropuesto(long idPrecioPropuesto, BigDecimal precioPropuesto);

    /**
     * ACCION 'C': aplica la propuesta. Vuelca cada precioPropuesto sobre tpr_precio.precio
     * y copia el costo sugerido de la propuesta a tpr_producto.costoTM, marcando
     * tpr_producto.idPropuestaAprobada. <b>Escribe fuera de tpr_precioPropuesta</b>: es la
     * operacion que "cierra" el reprecio.
     */
    RespuestaSp aplicarPreciosDePropuesta(long idPropuesta);

    /**
     * ACCION 'E': re-sincroniza el porcentaje de las filas de una propuesta y una familia
     * con el porcentaje vigente en tpr_porcentaje (por clasificacion de precio).
     */
    RespuestaSp sincronizarPorcentaje(long idPropuesta, String codigoFamilia, long audUsuario);

    // ────────────────────────── Listados (p_list_precioPropuesta) ──────────────────────

    /**
     * ACCION 'L': listado crudo de la tabla. Solo se envian los campos no nulos del filtro;
     * el resto queda en DEFAULT NULL = sin filtro.
     */
    List<PrecioPropuesta> listar(PrecioPropuesta filtro);

    /** ACCION 'L' filtrando por PK. Devuelve null si no existe. */
    PrecioPropuesta obtenerPorId(long idPrecioPropuesto);

    /** ACCION 'L' filtrando por propuesta. */
    List<PrecioPropuesta> listarPorPropuesta(long idPropuesta);

    /**
     * ACCION 'A': detalle de la propuesta para pantalla, con articulo, proveedor,
     * grupo de familia, sucursal y la formula de calculo ya armada.
     */
    List<PrecioPropuestaADto> listarDetallePorPropuesta(long idPropuesta);

    /**
     * ACCION 'B': comparativo horizontal (PIVOT) precios actuales vs propuestos,
     * una columna por lista de precios.
     */
    List<PrecioPropuestaBDto> listarComparativo(long idPropuesta);

    /**
     * ACCION 'C': indica si el precio ya fue cargado en esa propuesta. Sirve para no
     * permitir dos veces el mismo precio en una propuesta.
     */
    boolean existePrecioEnPropuesta(long idPropuesta, long idPrecio);

    /**
     * ACCION 'D': filas de una propuesta para una familia, con sucursal, lista de precios
     * y porcentaje resueltos por JOIN. Es lo que se edita en la pantalla de precios.
     */
    List<PrecioPropuestaDDto> listarPorPropuestaYFamilia(long idPropuesta, String codigoFamilia);
}
