package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CostoIncreCiudadDto;
import bo.bosque.com.impexpap.dto.CostoIncreSucursalDto;
import bo.bosque.com.impexpap.model.CostoIncre;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Contrato de acceso a la tabla tpr_costoIncre (costo de incremento / flete de
 * transporte por sucursal). Todo pasa por procedimientos almacenados:
 * p_abm_costoIncre para las escrituras y p_list_costoIncre para las lecturas.
 *
 * <p>Hay un metodo por ACCION real de cada procedimiento.
 */
public interface ICostoIncre {

    /**
     * ACCION 'I' de p_abm_costoIncre: inserta un costo de flete.
     *
     * @param costoIncre datos a grabar; idIncre se ignora (la PK es IDENTITY) y
     *                   audFecha la pone el SP con GETDATE().
     * @return RespuestaSp con el idIncre nuevo en getIdGenerado().
     */
    RespuestaSp insertar(CostoIncre costoIncre);

    /**
     * ACCION 'U' de p_abm_costoIncre: actualiza un costo existente.
     *
     * <p>El SP solo pisa valor, audUsuario y audFecha; codSucursal e idPropuesta
     * NO se pueden cambiar por esta via.
     *
     * @param costoIncre debe traer idIncre; sin el, el SP responde error 50.
     */
    RespuestaSp actualizar(CostoIncre costoIncre);

    /**
     * ACCION 'D' de p_abm_costoIncre: borra fisicamente el costo.
     *
     * @param idIncre PK a eliminar; obligatorio.
     */
    RespuestaSp eliminar(Long idIncre);

    /**
     * ACCION 'L' de p_list_costoIncre: listado plano de tpr_costoIncre.
     *
     * @param idIncre filtro opcional por PK; null devuelve todas las filas.
     * @return filas de la tabla (idIncre, codSucursal, valor, audUsuario,
     *         audFecha, idPropuesta).
     */
    List<CostoIncre> listar(Long idIncre);

    /**
     * ACCION 'B' de p_list_costoIncre: costos cargados con el nombre de la
     * sucursal (JOIN a tb_sucursal). Sin filtros: devuelve los costos de TODAS
     * las propuestas. Para una propuesta puntual usar
     * {@link #listarPorPropuesta(Long)}.
     */
    List<CostoIncreSucursalDto> listarCostoPorSucursal();

    /**
     * ACCION 'C' de p_list_costoIncre: catalogo de sucursales con su ciudad y
     * costo en 0, para que el formulario cargue los fletes de una propuesta
     * nueva. No lee tpr_costoIncre.
     */
    List<CostoIncreCiudadDto> listarSucursalesParaCarga();

    /**
     * ACCION 'D' de p_list_costoIncre: costos de flete de una propuesta con el
     * nombre de cada sucursal.
     *
     * @param idPropuesta obligatorio; en null el SP no devuelve filas.
     */
    List<CostoIncreSucursalDto> listarPorPropuesta(Long idPropuesta);
}
