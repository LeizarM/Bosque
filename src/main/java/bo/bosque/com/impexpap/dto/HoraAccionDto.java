package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una entrega del dia, para elegir cual copiar: fila de {@code p_list_Cheque} 'I' (custodias) o 'G'
 * (traspasos). {@code codAccion} es una de las acciones de esa entrega y {@code hora} es
 * "HH:mm" (con "Entregado a ..." en las custodias). Los nombres coinciden con los alias del SELECT.
 */
@Getter
@Setter
@NoArgsConstructor
public class HoraAccionDto implements Serializable {

    private Integer codAccion;
    private String hora;
}
