package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkTipoTicket;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Tipos de vale de un evento (p_list_ttk_TipoTicket / p_abm_ttk_TipoTicket). */
public interface ITtkTipoTicket {

    /** L: por codEvento o un tipo (codTipoTicket). */
    List<TtkTipoTicket> listar(TtkTipoTicket filtro);

    /** I (con codEvento) / U con los datos; D solo con codTipoTicket. */
    RespuestaSp registrar(TtkTipoTicket mb, String accion);
}
