package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TtkEmpleadoBingo;
import bo.bosque.com.impexpap.dto.TtkRankingCarton;
import bo.bosque.com.impexpap.model.TtkBingoCarton;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Cartones de un bingo (p_list_ttk_BingoCarton / p_abm_ttk_BingoCarton). */
public interface ITtkBingoCarton {

    /** L: cartones con su dueño (encargado); filtros codCarton, codEmpleado, codParticipante, soloDisponibles y buscar. */
    List<TtkBingoCarton> listar(TtkBingoCarton filtro);

    /** D: cartones libres, sin dueños, mientras la elección está abierta. */
    List<TtkBingoCarton> disponibles(long codBingo);

    /** M: cartones del empleado del usuario que llama, de un bingo o de todos. */
    List<TtkBingoCarton> mios(Long codBingo, long audUsuario);

    /** E: quienes pueden recibir cartón (con vale de bingo, participantes con vales; sin él, empleados activos). */
    List<TtkEmpleadoBingo> empleados(TtkBingoCarton filtro);

    /** R: ranking del juego en curso (top del bingo más todos los que ya completaron). */
    List<TtkRankingCarton> ranking(long codBingo);

    /** V: verificación de un cartón gritado (codBingo + nroCarton o codCarton); el SP siempre da una fila con mensaje. */
    TtkRankingCarton validar(TtkBingoCarton filtro);

    /** G (codBingo), E / X (el usuario elige o suelta), A (asigna a codParticipante o codEmpleado) y L (libera). */
    RespuestaSp registrar(TtkBingoCarton mb, String accion);
}
