package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.AutorizacionDto;
import bo.bosque.com.impexpap.dto.AutorizacionEmailDto;
import bo.bosque.com.impexpap.model.Autorizacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.Tipos;

/**
 * Contrato de acceso a datos de <b>tpr_autorizacion</b>.
 * Un metodo por ACCION real de p_abm_autorizacion y de p_list_autorizacion.
 */
public interface IAutorizacion {

    // ==================== p_abm_autorizacion ====================

    /**
     * ABM basico de la autorizacion. Invoca p_abm_autorizacion con ACCION
     * 'I' (insertar), 'U' (actualizar) o 'D' (eliminar).
     *
     * @param mb  registro con las columnas de tpr_autorizacion
     * @param acc "I", "U" o "D"
     * @return respuesta del SP; en 'I' trae el idAutorizacion nuevo en idGenerado
     */
    RespuestaSp registrarAutorizacion(Autorizacion mb, String acc);

    /**
     * ACCION 'A': resuelve una propuesta (aprobar / rechazar / dejar pendiente o en espera).
     * Si esAprobada es 0 (Pendiente) o 3 (En Espera) el SP limpia audUsuario y audFecha;
     * en cualquier otro caso graba el usuario y la fecha del servidor.
     * Cuando la propuesta es de tipo 2 (por articulo) y queda Aprobada, el SP ademas
     * copia el detalle al historico tpr_bitArticulo.
     *
     * @param idPropuesta propuesta a resolver
     * @param esAprobada  0 Pendiente, 1 Aprobada, 2 No Aprobada, 3 En Espera
     * @param audUsuario  usuario que resuelve
     * @return respuesta del SP
     */
    RespuestaSp resolverPropuesta(Long idPropuesta, Integer esAprobada, Long audUsuario);

    /**
     * ACCION 'B': deja la autorizacion de la propuesta en estado 3 (En Espera)
     * y limpia audFecha.
     *
     * @param idPropuesta propuesta afectada
     * @return respuesta del SP
     */
    RespuestaSp marcarEnEspera(Long idPropuesta);

    // ==================== p_list_autorizacion ====================

    /**
     * ACCION 'L': filas crudas de tpr_autorizacion. Cada filtro en null se ignora.
     *
     * @param idAutorizacion filtro opcional por PK
     * @param idPropuesta    filtro opcional por propuesta
     * @param audUsuario     filtro opcional por usuario que autorizo
     * @return lista de registros de la tabla
     */
    List<Autorizacion> listarAutorizacion(Long idAutorizacion, Long idPropuesta, Long audUsuario);

    /**
     * ACCION 'A': vista principal de propuestas por autorizar (TOP 100,
     * mas reciente primero), con titulo de la propuesta, estado legible y
     * nombres de las personas involucradas.
     *
     * @return filas de la vista principal
     */
    List<AutorizacionDto> listAutorizacion();

    /**
     * ACCION 'B': correos registrados del usuario indicado.
     *
     * @param audUsuario codUsuario de tb_usuario
     * @return correos encontrados
     */
    List<AutorizacionEmailDto> obtenerEmailUsuario(Long audUsuario);

    /**
     * ACCION 'C': correos (&#64;impexpap.com) del usuario que creo la propuesta.
     *
     * @param idPropuesta propuesta de referencia
     * @return correos encontrados
     */
    List<AutorizacionEmailDto> obtenerEmailPropuesta(Long idPropuesta);

    /**
     * ACCION 'D': correos (&#64;impexpap.com) de los empleados con la funcion
     * AUTORIZADOR en el modulo de precios.
     *
     * @return correos de los autorizadores
     */
    List<AutorizacionEmailDto> obtenerEmailAutorizadores();

    // ==================== catalogo en memoria ====================

    /**
     * Estados posibles de una propuesta (v_tipos grupo 38). No toca la base de datos.
     *
     * @return catalogo de estados
     */
    List<Tipos> lstEstadoPropuestas();
}
