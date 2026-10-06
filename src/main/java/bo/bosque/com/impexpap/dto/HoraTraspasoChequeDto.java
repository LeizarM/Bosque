package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un traspaso de un dia para elegir cual reimprimir ({@code p_list_Cheque} 'G'): el codigo de su accion y la
 * hora ({@code HH:mm}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoraTraspasoChequeDto implements Serializable {

    private Integer codAccion;
    private String hora;
}
