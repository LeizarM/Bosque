package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Cuerpo de {@code /cheque/custodia} ("A Custodio"): entrega varios cheques al responsable elegido.
 * Cada cheque marcado recibe una accion {@code CUS}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class CustodiaChequeRequest implements Serializable {

    private Long codSucursal;

    /** Jefe de cobranzas o cobrador que se lleva los cheques. Obligatorio. */
    private Integer codEmpleado;

    private List<Integer> codCheques;
}
