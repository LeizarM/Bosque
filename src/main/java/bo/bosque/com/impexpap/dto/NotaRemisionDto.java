package bo.bosque.com.impexpap.dto;

import bo.bosque.com.impexpap.model.ChNotaRemision;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Una nota de remision en la lista del detalle del cheque ({@code p_list_NotaRemision} 'L' por {@code codCheque}).
 * Hereda las columnas de la tabla y agrega el numero de fila de la pantalla (el "Nro" del legacy).
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
public class NotaRemisionDto extends ChNotaRemision {

    /** Numero de fila, 1..n, en el orden del procedimiento. Lo pone el DAO. */
    private Integer fila;
}
