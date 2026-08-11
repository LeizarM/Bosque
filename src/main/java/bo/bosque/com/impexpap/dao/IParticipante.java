package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Participante;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

/**
 * La foto del organigrama dentro del rol.
 * SPs: {@code p_abm_trs_Participante} · {@code p_list_trs_Participante}.
 *
 * <p><b>"No está" son dos hechos distintos y cada uno tiene su columna:</b>
 * {@code activo} dice si sigue en la relación laboral vigente y lo escribe sólo
 * {@code trs_sp_generarRol}; {@code fechaAlta}/{@code fechaBaja} dicen desde y hasta
 * cuándo hace sábados y las escribe sólo RR.HH., por {@link #sacarDeSabados} y
 * {@link #reincorporar}. Antes compartían el bit y la baja manual se deshacía sola
 * en la siguiente regeneración.
 */
public interface IParticipante {

    /**
     * Alta ('I') o modificación ('U') de un participante.
     *
     * <p>Al agregar uno nuevo hay que correr después
     * {@code trs_sp_generarRol @modo='REGENERAR'} para que se le generen las celdas;
     * el SP lo avisa en {@code errormsg}.
     *
     * <p><b>El {@code 'U'} ya no toca {@code activo}</b> aunque el modelo lo mande:
     * el SP lo ignora a propósito. {@code Participante.activo} está inicializado en 1,
     * así que cualquier edición reactivaba a alguien que la reconciliación había dado
     * de baja. Para sacar o devolver a alguien de los sábados están los dos métodos
     * de abajo, no este.
     *
     * @param participante Datos del participante
     * @param acc          'I' | 'U'
     */
    RespuestaSp registrarParticipante(Participante participante, String acc);

    /**
     * <b>Saca a una persona de los sábados</b> a partir de una fecha, sin borrarle
     * nada de lo que ya pasó ({@code @ACCION='D'}).
     *
     * <p>Cierra la ventana: {@code fechaBaja}. NO escribe {@code activo} — esa
     * persona sigue trabajando en la empresa, sólo que no viene los sábados.
     *
     * <p>Qué pasa con las celdas: las <b>pasadas no se tocan</b> (eso es el
     * softdelete), las futuras {@code 'G'} se borran, y las futuras {@code 'P'} (las
     * programó un jefe) y {@code 'M'} (las corrigió RR.HH.) <b>SOBREVIVEN y se
     * cuentan</b> — el mensaje de respuesta dice cuántas quedaron para que alguien
     * las revise. Pisarlas invertiría la regla que sostiene el módulo entero.
     *
     * <p>Aplica sus propias celdas y NO necesita REGENERAR, que sobre un rol
     * PUBLICADO está bloqueado.
     *
     * @param fechaBaja hasta cuándo viene, inclusive. NULL = hoy
     */
    RespuestaSp sacarDeSabados(long idParticipante, Date fechaBaja, long audUsuario);

    /**
     * <b>Devuelve a una persona a los sábados</b> a media gestión ({@code @ACCION='R'}).
     *
     * <p>Abre la ventana ({@code fechaAlta}, y borra {@code fechaBaja}) y le escribe
     * las celdas de ahí en adelante sábado por sábado, sin tocar el pasado y sin
     * rehacer las de nadie más.
     *
     * <p>Rebota si la persona ya no figura en la relación laboral vigente
     * ({@code activo=0}): devolverla con un clic sería saltearse a RR.HH.
     *
     * @param fechaAlta desde cuándo vuelve. NULL = el próximo sábado del rol
     */
    RespuestaSp reincorporar(long idParticipante, Date fechaAlta, long audUsuario);

    /**
     * Cambia el grupo de rotación de UNA persona.
     *
     * <p>Va por el camino del Map y no por el del modelo a propósito: el ABM
     * actualiza cada campo con {@code ISNULL(@campo, campo)}, así que mandar el
     * modelo entero pisaría con los valores por defecto de Java todo lo que no
     * se cargó. Acá viajan tres parámetros y nada más.
     *
     * <p>El cambio NO mueve la grilla: las celdas ya escritas siguen igual hasta
     * que se corra {@code REGENERAR}.
     *
     * @param grupoRotacion 'A' o 'B'
     */
    RespuestaSp asignarGrupo(long idParticipante, String grupoRotacion, long audUsuario);

    /** [L] Los participantes de un rol. @param activo -1 = todos, 0/1 = filtra */
    List<Participante> obtenerParticipantes(long idRol, int activo);

    /** [L] Un participante por su id. */
    Participante obtenerParticipantePorId(long idParticipante);

    /** [T] Turnos por persona: el SUM de la derecha del Excel. */
    List<Participante> obtenerTurnosPorParticipante(long idRol);

    /**
     * [K] Cumpleaños que caen sábado y qué pasó con el asueto.
     * {@code situacionAsueto} distingue ASUETO · ASUETO ANULADO POR EVENTO ·
     * LO EXCUSARON · DECIDIO VENIR · FERIADO — es la lista que RR.HH. necesita
     * para saber a quién compensar.
     */
    List<Participante> obtenerCumplesSabado(long idRol);
}
