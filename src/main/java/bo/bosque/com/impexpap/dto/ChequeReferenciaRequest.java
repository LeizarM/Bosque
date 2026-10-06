package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Un cheque por su codigo interno ({@code tch_cheque.codCheque}), para listar sus notas, transacciones o postergaciones. */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ChequeReferenciaRequest implements Serializable {

    private Integer codCheque;
}
