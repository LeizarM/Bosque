package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CostoSug;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Contrato de acceso a datos de <b>tpr_costoSug</b> (costo sugerido por familia
 * dentro de una propuesta de precios).
 *
 * <p>Todo pasa por procedimientos almacenados: {@code p_abm_costoSug} para la
 * escritura y {@code p_list_costSug} para la lectura. El nombre del listado va
 * sin la "o" de "costo" a proposito: asi lo llama el JSF legacy.
 */
public interface ICostoSug {

    /**
     * Registra el costo sugerido de una familia — accion <b>I</b> de
     * {@code p_abm_costoSug}.
     *
     * <p>Cuidado con el nombre: la accion I del procedimiento es en realidad un
     * <i>upsert</i>. Busca si ya existe una fila para el par
     * (idPropuesta, codigoFamilia); si existe actualiza esa fila en lugar de
     * insertar otra. {@code fechaI} y {@code audFecha} las pisa con GETDATE().
     *
     * @param mb datos del costo sugerido (idPropuesta y codigoFamilia obligatorios)
     * @return respuesta del procedimiento; {@code idGenerado} trae el idCosSug
     *         insertado, o el de la fila actualizada cuando el upsert cayo en update
     */
    RespuestaSp registrar(CostoSug mb);

    /**
     * Actualiza una fila existente por su PK — accion <b>U</b> de
     * {@code p_abm_costoSug}. A diferencia de la accion I, aqui si se respeta el
     * {@code fechaI} que venga en el modelo. {@code audFecha} la pisa GETDATE().
     *
     * @param mb datos del costo sugerido, con {@code idCosSug} cargado
     * @return respuesta del procedimiento
     */
    RespuestaSp actualizar(CostoSug mb);

    /**
     * Borra los costos sugeridos de una propuesta — accion <b>D</b> de
     * {@code p_abm_costoSug}.
     *
     * <p>El nombre es explicito porque el procedimiento borra <b>todas</b> las filas
     * de la propuesta, no una sola: su WHERE es por {@code idPropuesta} y el
     * {@code idCosSug} lo ignora. Es el comportamiento que espera el JSF (rehace el
     * bloque de costos completo al reabrir una propuesta) y se conserva tal cual.
     *
     * @param idPropuesta propuesta cuyos costos sugeridos se eliminan; obligatorio
     * @param audUsuario  usuario que ejecuta la baja (auditoria); puede ser null
     * @return respuesta del procedimiento
     */
    RespuestaSp eliminarPorPropuesta(Long idPropuesta, Long audUsuario);

    /**
     * Lista todos los costos sugeridos — accion <b>L</b> de {@code p_list_costSug}
     * sin filtros.
     *
     * @return filas de tpr_costoSug; lista vacia si no hay
     */
    List<CostoSug> listar();

    /**
     * Obtiene un costo sugerido por su PK — accion <b>L</b> filtrando por
     * {@code @idCosSug}.
     *
     * @param idCosSug PK a buscar
     * @return la fila, o null si no existe
     */
    CostoSug obtenerPorId(Long idCosSug);

    /**
     * Obtiene los costos sugeridos de una propuesta — accion <b>L</b> filtrando por
     * {@code @idPropuesta}.
     *
     * @param idPropuesta propuesta a consultar
     * @return filas de esa propuesta; lista vacia si no hay
     */
    List<CostoSug> obtenerPorPropuesta(Long idPropuesta);

    /**
     * Obtiene el costo sugerido de una familia dentro de una propuesta — accion
     * <b>L</b> filtrando por {@code @idPropuesta} y {@code @codigoFamilia}. Es la
     * misma clave que usa el upsert de la accion I.
     *
     * @param idPropuesta   propuesta a consultar
     * @param codigoFamilia familia de producto
     * @return la fila, o null si esa familia todavia no tiene costo sugerido
     */
    CostoSug obtenerPorPropuestaYFamilia(Long idPropuesta, Integer codigoFamilia);

    /**
     * Puente de compatibilidad con el controlador legacy
     * ({@code PrecioController.registrarCostoSug}), que decide la accion
     * por su cuenta y solo mira un booleano.
     *
     * @param costoSug datos del costo sugerido
     * @param acc      accion: 'I', 'U' o 'D'
     * @return true si el procedimiento termino sin error
     * @deprecated usar {@link #registrar(CostoSug)}, {@link #actualizar(CostoSug)} o
     *             {@link #eliminarPorPropuesta(Long, Long)}, que devuelven
     *             {@link RespuestaSp} con el mensaje de error real y el id generado.
     */
    @Deprecated
    boolean registrarCostoSug(CostoSug costoSug, String acc);
}
