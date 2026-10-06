package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * El cheque cuyo documento PDF se consulta o se descarga ({@code POST /cheque/pdf/estado} y {@code /cheque/pdf/descargar}).
 * {@code codCheque} es el codigo interno ({@code tch_cheque.codCheque}), no el numero impreso en el cheque.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChequePdfRequest implements Serializable {

    private Integer codCheque;
}
