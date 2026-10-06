package bo.bosque.com.impexpap.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import bo.bosque.com.impexpap.model.ChTransaccionBancaria;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Una transaccion bancaria en la lista del detalle del cheque. Es la rama 'A' de {@code p_list_ChTransaccionBancaria},
 * que se lee POR POSICION como el legacy:
 * <pre>
 *   1 codCheque   2 nroTransaccion   3 codBanco   4 fechaTransaccion   5 datoBanco   6 datoFecha
 * </pre>
 * La rama hace {@code JOIN} con {@code tch_banco}: una transaccion cuyo banco ya no existe no aparece. No trae
 * {@code audUsuario} ni {@code audFecha}: no salen en el JSON ({@code NON_NULL}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class TransaccionBancariaDto extends ChTransaccionBancaria {

    /** Numero de fila, 1..n, en el orden del procedimiento. Lo pone el DAO. */
    private Integer fila;

    /** Nombre del banco ({@code tch_banco.nombre}). */
    private String datoBanco;
}
