package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TtkBingoParticipante;
import bo.bosque.com.impexpap.dto.TtkBingoPublico;
import bo.bosque.com.impexpap.model.TtkBingo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Bingo de un evento (p_list_ttk_Bingo / p_abm_ttk_Bingo). */
public interface ITtkBingo {

    /** L: configuración y avance para el encargado, por codBingo o codEvento. */
    List<TtkBingo> listar(TtkBingo filtro);

    /** A: bingos visibles para el empleado del usuario que llama. */
    List<TtkBingoParticipante> misBingos(long audUsuario);

    /** P: estado público (sin ganadores); null si el bingo no existe. */
    TtkBingoPublico publico(long codBingo);

    /** I (codEvento) / U (codBingo) con la configuración y codTipoTicket si viene; E, J, F y D solo con codBingo. */
    RespuestaSp registrar(TtkBingo mb, String accion);
}
