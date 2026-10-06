package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.NotaRemisionDto;
import bo.bosque.com.impexpap.model.ChNotaRemision;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/** Acceso a {@code tch_notaRemision} por {@code p_list_NotaRemision} y {@code p_abm_NotaRemision}. */
public interface IChNotaRemision {

    /**
     * {@code p_list_NotaRemision} 'L' por {@code codCheque}: las notas del cheque, en el orden del procedimiento (la tabla es
     * un heap sin {@code ORDER BY}), numeradas 1..n. Las repetidas (mismo cheque y misma nota) salen todas.
     */
    List<NotaRemisionDto> listarPorCheque(int codCheque);

    /** {@code p_abm_NotaRemision} 'I'. No hay id propio: {@code idGenerado} = filas insertadas. */
    RespuestaSp registrar(ChNotaRemision nota);

    /**
     * {@code p_abm_NotaRemision} 'D': borra por ({@code codCheque}, {@code notaRemision}) <b>todas</b> las filas que
     * coincidan, como el legacy. {@code idGenerado} = filas borradas.
     */
    RespuestaSp eliminar(int codCheque, String notaRemision, int audUsuario);
}
