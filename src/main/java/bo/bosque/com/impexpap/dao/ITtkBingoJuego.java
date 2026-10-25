package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkBingoJuego;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Juegos (rondas) de un bingo con su figura (p_list_ttk_BingoJuego / p_abm_ttk_BingoJuego). */
public interface ITtkBingoJuego {

    /** L: por codBingo o un juego (codJuego). */
    List<TtkBingoJuego> listar(TtkBingoJuego filtro);

    /** U: nombre y figura; C (comenzar) y F (finalizar) solo con codJuego. */
    RespuestaSp registrar(TtkBingoJuego mb, String accion);
}
