package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TtkBingoAviso;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Avisos «¡Bingo!» de los participantes (p_list_ttk_BingoAviso / p_abm_ttk_BingoAviso). */
public interface ITtkBingoAviso {

    /** L: pendientes del juego en curso de un bingo, sin los cartones ya confirmados como ganadores. */
    List<TtkBingoAviso> pendientes(long codBingo);

    /** L con codAviso: ese aviso en cualquier estado, o null si no existe. */
    TtkBingoAviso buscar(long codAviso);

    /** I: el dueño (audUsuario) avisa con codCarton; A: el encargado atiende codAviso. */
    RespuestaSp registrar(TtkBingoAviso mb, String accion);
}
