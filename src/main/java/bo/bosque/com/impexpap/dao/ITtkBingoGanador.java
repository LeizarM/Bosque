package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkBingoGanador;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Ganadores confirmados por el encargado (p_list_ttk_BingoGanador / p_abm_ttk_BingoGanador). */
public interface ITtkBingoGanador {

    /** L: ganadores de un bingo, de un juego o una confirmación (codGanador). */
    List<TtkBingoGanador> listar(TtkBingoGanador filtro);

    /** P: confirmados de un juego para la pantalla en vivo (nombre y codEmpleado para la foto, sin cartón). */
    List<TtkBingoGanador> publicos(long codBingo, int nroJuego);

    /**
     * I: confirma el cartón (codBingo + nroCarton o codCarton) en el juego en curso; D: anula codGanador;
     * E: codGanador ganó el desempate entre los confirmados del juego, por motivoDesempate.
     */
    RespuestaSp registrar(TtkBingoGanador mb, String accion);
}
