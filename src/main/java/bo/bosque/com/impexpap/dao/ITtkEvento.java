package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkEvento;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Eventos del módulo de tickets (p_list_ttk_Evento / p_abm_ttk_Evento). */
public interface ITtkEvento {

    /** L: todos o uno, con filtros opcionales de estado y año. */
    List<TtkEvento> listar(TtkEvento filtro);

    /** I/U con los datos del evento; D, E (emitir), B (volver a borrador) y C (cerrar) solo con codEvento. */
    RespuestaSp registrar(TtkEvento mb, String accion);
}
