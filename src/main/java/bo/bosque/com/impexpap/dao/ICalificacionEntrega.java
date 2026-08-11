package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.ResumenCalificacionDTO;
import bo.bosque.com.impexpap.model.CalificacionEntrega;

import java.util.Date;
import java.util.List;

/**
 * Acceso a datos de las calificaciones de entrega (tabla trch_EntregaCalificacion).
 *
 * <p><b>NINGUN metodo de esta interfaz lanza excepcion.</b> No es una recomendacion, es el
 * contrato. Dos de los cuatro metodos cuelgan del flujo de WhatsApp:
 * {@link #registrarCalificacion(CalificacionEntrega, String)} con ACCION 'I' se invoca colgado del
 * registro de entrega del chofer, y {@link #buscarPendientePorChat(String, int)} se invoca desde el
 * webhook publico, que recibe mensajes de cualquiera. Si el SP todavia no esta desplegado, si la
 * base no responde o si entra basura, estos metodos devuelven false / null / lista vacia y lo
 * loguean; jamas propagan la falla a quien registra entregas ni tumban el webhook.</p>
 *
 * <p>La implementacion tampoco anula nunca el JdbcTemplate en el catch (ver el comentario largo en
 * {@code EntregaChoferDao} sobre por que ese antipatron mata el bean singleton para siempre).</p>
 */
public interface ICalificacionEntrega {

    /**
     * Escribe una calificacion via el SP p_abm_trch_EntregaCalificacion.
     *
     * <p>Acciones previstas:</p>
     * <ul>
     *   <li><b>'I'</b> - alta de la encuesta al momento de mandarle el aviso al cliente. Deja la
     *       fila pendiente: con chatId cargado y puntaje / fechaRespuesta en NULL.</li>
     *   <li><b>'U'</b> - actualizacion con la respuesta del cliente. Completa puntaje, comentario,
     *       mensajeCrudo y fechaRespuesta sobre la fila pendiente, ubicada por
     *       {@code idCalificacion}.</li>
     * </ul>
     *
     * <p>NUNCA lanza excepcion.</p>
     *
     * @param mb     la calificacion; si es null se devuelve false sin tocar la base
     * @param accion 'I' o 'U'; si es null o vacia se devuelve false sin tocar la base
     * @return true si el SP afecto al menos una fila; false si no afecto ninguna (por ejemplo, un
     *         'U' cuyo idCalificacion ya no existe) o si hubo cualquier error
     */
    boolean registrarCalificacion( CalificacionEntrega mb, String accion );

    /**
     * Busca la encuesta PENDIENTE mas reciente de un chat (SP p_list_trch_EntregaCalificacion
     * ACCION 'P').
     *
     * <p>"Pendiente" es una fila con {@code puntaje IS NULL} cuya {@code fechaEntrega} cae dentro
     * de las ultimas {@code horasVentana} horas. La ventana existe para que un cliente que escribe
     * "5" tres semanas despues, por cualquier otro motivo, no termine calificando una entrega
     * vieja.</p>
     *
     * <p>Este metodo es el que recibe el rebote del webhook publico, asi que {@code chatId} es dato
     * NO CONFIABLE: llega desde afuera. La implementacion lo manda siempre como parametro
     * enlazado, nunca concatenado al SQL.</p>
     *
     * <p>NUNCA lanza excepcion.</p>
     *
     * @param chatId       chat de WhatsApp normalizado ({@code 591XXXXXXXX@c.us})
     * @param horasVentana antiguedad maxima, en horas, de la entrega a calificar
     * @return la calificacion pendiente, o null si no hay ninguna (el caso normal: la mayoria de
     *         los mensajes entrantes no corresponden a ninguna encuesta) o si hubo un error
     */
    CalificacionEntrega buscarPendientePorChat( String chatId, int horasVentana );

    /**
     * Listado de calificaciones para el reporte de detalle (SP p_list_trch_EntregaCalificacion
     * ACCION 'L').
     *
     * <p>NUNCA lanza excepcion.</p>
     *
     * @param desde    inicio del rango; null = sin limite inferior
     * @param hasta    fin del rango; null = sin limite superior
     * @param uChofer  codigo del chofer a filtrar; <b>0 = todos los choferes</b>
     * @return la lista, o una lista VACIA (nunca null) si no hay datos o si hubo un error
     */
    List<CalificacionEntrega> listarCalificaciones( Date desde, Date hasta, int uChofer );

    /**
     * Reporte agregado por chofer (SP p_list_trch_EntregaCalificacion ACCION 'R').
     *
     * <p>NUNCA lanza excepcion.</p>
     *
     * @param desde inicio del rango; null = sin limite inferior
     * @param hasta fin del rango; null = sin limite superior
     * @return una fila por chofer, o una lista VACIA (nunca null) si no hay datos o si hubo un error
     */
    List<ResumenCalificacionDTO> resumenPorChofer( Date desde, Date hasta );

}
