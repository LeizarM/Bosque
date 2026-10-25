package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TtkValeImpresion;
import bo.bosque.com.impexpap.dto.TtkValeLote;
import bo.bosque.com.impexpap.model.TtkParticipanteTicket;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Vales por tipo de cada participante (p_list_ttk_ParticipanteTicket / p_abm_ttk_ParticipanteTicket). */
public interface ITtkParticipanteTicket {

    /** L: vales de un participante o de todo el evento. */
    List<TtkParticipanteTicket> listar(TtkParticipanteTicket filtro);

    /** P: un renglón por vale numerado del evento, opcionalmente de un participante o un tipo. */
    List<TtkValeImpresion> impresion(TtkParticipanteTicket filtro);

    /** U: fija la cantidad de un tipo a un participante; R: vuelve a la cantidad por defecto. */
    RespuestaSp registrar(TtkParticipanteTicket mb, String accion);

    /** M: un tipo de vale para varios participantes (lista, categoría o todos); cantidad null = la por defecto. */
    RespuestaSp cambiarLote(TtkValeLote lote, long audUsuario);
}
