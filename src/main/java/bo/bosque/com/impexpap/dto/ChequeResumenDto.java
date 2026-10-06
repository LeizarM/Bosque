package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un cheque en una lista de seleccion: fila de {@code p_list_Cheque} 'J'
 * ({@code codCheque}, {@code datoCheque} = "Nro Cheque : ..., Cliente : ..., Monto (BS) : ...").
 * Los nombres coinciden con los alias del SELECT.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChequeResumenDto implements Serializable {

    private Integer codCheque;
    private String datoCheque;
}
