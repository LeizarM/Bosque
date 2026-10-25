package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkBingoSorteo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Bolillos que salieron de la máquina (p_list_ttk_BingoSorteo / p_abm_ttk_BingoSorteo). */
public interface ITtkBingoSorteo {

    /** T: tablero completo del juego en curso (o del último terminado), con marcado y orden. */
    List<TtkBingoSorteo> tablero(long codBingo);

    /** I: marca nroBolillo en el juego en curso; D: lo quita. El mensaje avisa si hubo bingo. */
    RespuestaSp registrar(TtkBingoSorteo mb, String accion);
}
