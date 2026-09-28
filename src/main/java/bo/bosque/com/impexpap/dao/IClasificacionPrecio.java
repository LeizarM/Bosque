package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.ClasificacionPrecioDto;
import bo.bosque.com.impexpap.model.ClasificacionPrecio;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Acceso a datos de tpr_clasificacionPrecio (listas de precios por sucursal).
 *
 * <p>Todo pasa por procedimientos almacenados: {@code p_abm_clasificacionPrecio} y
 * {@code p_list_clasificacionPrecio}. Hacia afuera esta interfaz expone las acciones
 * estandar I/U/D; la traduccion a las letras historicas del SP ('B' para el alta) queda
 * encapsulada en el DAO.
 */
public interface IClasificacionPrecio {

    /**
     * Alta, modificacion o baja de una lista de precios.
     *
     * @param mb  datos de la lista de precios
     * @param acc accion estandar: {@code "I"} alta (el DAO la traduce a la ACCION 'B' del SP),
     *            {@code "U"} modificacion de nombre y estado, {@code "D"} baja fisica
     * @return respuesta del SP; en el alta trae el id nuevo en {@code getIdGenerado()}
     */
    RespuestaSp registrarClasificacionPrecio(ClasificacionPrecio mb, String acc);

    /**
     * Activa o desactiva una lista de precios sin tocar el resto de los campos.
     * Invoca la ACCION 'A' del SP de ABM, que solo escribe estado y auditoria.
     *
     * @param idClasificacion PK de la lista de precios
     * @param estado          1 = activo, 0 = desactivado
     * @param audUsuario      usuario que hace el cambio
     */
    RespuestaSp cambiarEstado(long idClasificacion, int estado, long audUsuario);

    /**
     * Baja fisica de una lista de precios. Invoca la ACCION 'D' del SP de ABM, que antes
     * verifica que no existan precios ni porcentajes asociados.
     *
     * @param idClasificacion PK de la lista de precios
     */
    RespuestaSp eliminarClasificacionPrecio(long idClasificacion);

    /**
     * Listado plano de listas de precios (ACCION 'L' del SP de listado).
     *
     * @param idClasificacion filtro por PK; null o 0 = todas
     * @param codSucursal     filtro por sucursal; null o 0 = todas
     * @return filas de tpr_clasificacionPrecio
     */
    List<ClasificacionPrecio> obtenerClasificacionPrecio(Long idClasificacion, Long codSucursal);

    /**
     * Listas de precios con el nombre de su sucursal (ACCION 'B' del SP de listado),
     * ordenadas por estado descendente y vpp.
     */
    List<ClasificacionPrecioDto> obtenerClasificacionPrecioConSucursal();

    /**
     * Todos los vpp (numeros de lista de precios) registrados, ordenados.
     * Invoca la ACCION 'C' del SP de listado.
     */
    List<Integer> obtenerVpps();

    /**
     * Indica si el vpp ya esta usado por otra lista de precios.
     * Invoca la ACCION 'D' del SP de listado, que NO es la baja: en el proc de listado
     * la letra 'D' significa "control de duplicados".
     *
     * @param vpp                 numero de lista a verificar
     * @param idClasificacionExcluir id a excluir de la verificacion (el registro que se esta
     *                               editando); null o 0 para no excluir ninguno
     * @return true si ya existe otra fila con ese vpp
     */
    boolean existeVpp(int vpp, Long idClasificacionExcluir);
}
