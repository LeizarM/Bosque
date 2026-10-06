package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Cuerpo de {@code /cheque/dar-custodia} ("Dar Custodia", administrador): copia una accion
 * {@code CUS} ya hecha a otro cheque, con la misma fecha, responsable y observacion.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class DarCustodiaRequest implements Serializable {

    private Long codSucursal;

    /** La accion CUS que se copia (de la lista de entregas del dia). */
    private Integer codAccionOrigen;

    /** El cheque que la recibe. */
    private Integer codCheque;
}
