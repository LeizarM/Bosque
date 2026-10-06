package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.TransaccionBancariaDto;
import bo.bosque.com.impexpap.model.ChTransaccionBancaria;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/** Acceso a {@code tch_chTransaccionBancaria} por {@code p_list_ChTransaccionBancaria} y {@code p_abm_ChTransaccionBancaria}. */
public interface IChTransaccionBancaria {

    /**
     * {@code p_list_ChTransaccionBancaria} 'A' por {@code codCheque}, POR POSICION (6 columnas): las transacciones del cheque
     * con el nombre del banco, numeradas 1..n. La rama hace {@code JOIN} con {@code tch_banco}.
     */
    List<TransaccionBancariaDto> listarPorCheque(int codCheque);

    /** {@code p_abm_ChTransaccionBancaria} 'I'. No hay id propio: {@code idGenerado} = filas insertadas. */
    RespuestaSp registrar(ChTransaccionBancaria transaccion);

    /**
     * {@code p_abm_ChTransaccionBancaria} 'D': borra por ({@code codCheque}, {@code nroTransaccion}) todas las filas que
     * coincidan, como el legacy. {@code idGenerado} = filas borradas.
     */
    RespuestaSp eliminar(int codCheque, String nroTransaccion, int audUsuario);
}
