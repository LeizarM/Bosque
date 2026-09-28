package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.PorcentajeDto;
import bo.bosque.com.impexpap.model.Porcentaje;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Acceso a datos de la tabla <b>tpr_porcentaje</b>.
 * Todo pasa por los procedimientos {@code p_abm_porcentaje} y
 * {@code p_list_porcentaje}; no hay SQL crudo.
 */
public interface IPorcentaje {

    /**
     * Alta, modificacion o baja de un porcentaje — p_abm_porcentaje.
     *
     * @param porcentaje datos del registro. En 'I' se usan codigoFamilia,
     *                   idClasificacion, porcen y audUsuario; en 'U' porcen,
     *                   audUsuario e idPorcen; en 'D' solo idPorcen.
     * @param acc        'I' (insertar), 'U' (actualizar) o 'D' (eliminar).
     * @return respuesta del procedimiento; en el alta trae el idPorcen generado.
     */
    RespuestaSp registrarPorcentaje(Porcentaje porcentaje, String acc);

    /**
     * Todos los porcentajes cargados — p_list_porcentaje ACCION 'L' sin filtros.
     *
     * @return filas de tpr_porcentaje.
     */
    List<Porcentaje> listarPorcentajes();

    /**
     * Un porcentaje por su clave — p_list_porcentaje ACCION 'L' filtrando por idPorcen.
     *
     * @param idPorcen clave de tpr_porcentaje.
     * @return el registro, o null si no existe.
     */
    Porcentaje obtenerPorcentajePorId(long idPorcen);

    /**
     * Porcentajes de una familia y/o de una lista de precios — p_list_porcentaje
     * ACCION 'L' filtrando. Los parametros en null no filtran.
     *
     * @param codigoFamilia   familia de producto, o null para no filtrar.
     * @param idClasificacion lista de precios, o null para no filtrar.
     * @return filas de tpr_porcentaje que cumplen el filtro.
     */
    List<Porcentaje> listarPorcentajesPorFamiliaYClasificacion(Integer codigoFamilia, Long idClasificacion);

    /**
     * Grilla de porcentajes vigentes de una familia, una fila por sucursal y
     * lista de precios — p_list_porcentaje ACCION 'A'.
     * Si la familia todavia no tiene porcentajes cargados el procedimiento
     * devuelve la misma grilla con idPorcen y porcentaje en 0 (alta pendiente).
     *
     * @param codigoFamilia familia de producto. Obligatorio.
     * @return filas para la grilla de la pantalla de porcentajes.
     */
    List<PorcentajeDto> listarPorcentajesPorFamilia(int codigoFamilia);

    /**
     * Sucursales con sus listas de precios activas para armar el ABM de
     * porcentajes desde cero — p_list_porcentaje ACCION 'B'.
     * No consulta tpr_porcentaje: idPorcen llega null y porcentaje en 0.
     *
     * @return filas para la grilla vacia de alta.
     */
    List<PorcentajeDto> listarSucursalesParaAbm();

    /**
     * Listas de precios sin porcentaje cargado para una familia —
     * p_list_porcentaje ACCION 'C'.
     *
     * @param codigoFamilia familia de producto. Obligatorio.
     * @return filas pendientes de completar.
     */
    List<PorcentajeDto> listarPorcentajesFaltantes(int codigoFamilia);
}
