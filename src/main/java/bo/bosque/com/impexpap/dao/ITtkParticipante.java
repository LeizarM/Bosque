package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkParticipante;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Participantes de un evento: empleados e invitados (p_list_ttk_Participante / p_abm_ttk_Participante). */
public interface ITtkParticipante {

    /** L: participantes del evento con sus vales; filtros categoria y buscar. */
    List<TtkParticipante> listar(TtkParticipante filtro);

    /** E: empleados activos que aún no están en el evento, con la categoría que les tocaría. */
    List<TtkParticipante> empleados(TtkParticipante filtro);

    /** G (codEvento), I / U (datos), D y A (codParticipante). */
    RespuestaSp registrar(TtkParticipante mb, String accion);
}
