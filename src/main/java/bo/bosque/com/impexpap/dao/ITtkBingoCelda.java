package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkBingoCelda;

import java.util.List;

/**
 * Casillas de los cartones (p_list_ttk_BingoCelda). Las escribe p_abm_ttk_BingoCelda, que solo
 * llaman otros SP al generar o rehacer cartones: no tiene endpoint propio.
 */
public interface ITtkBingoCelda {

    /** E: casillas de cualquier cartón (encargado). */
    List<TtkBingoCelda> delCarton(long codCarton);

    /** C: casillas de un cartón libre o del usuario que llama; de uno ajeno no devuelve nada. */
    List<TtkBingoCelda> verCarton(long codCarton, long audUsuario);

    /** M: casillas de todos los cartones del usuario que llama, de un bingo o de todos. */
    List<TtkBingoCelda> misCeldas(Long codBingo, long audUsuario);

    /** P: impresión del encargado por empleado, rango de números de cartón o solo asignados. */
    List<TtkBingoCelda> impresion(TtkBingoCelda filtro);
}
