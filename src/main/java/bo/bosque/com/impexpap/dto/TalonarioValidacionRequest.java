package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lo que el formulario manda para comprobar el par talonario/recibo <b>mientras se escribe</b>. {@code codEmpresa} es la
 * empresa del cheque (la de la pantalla en un alta, la del propio cheque en una edicion): un talonario solo vale para
 * la empresa a la que pertenece.
 */
@Getter
@Setter
@NoArgsConstructor
public class TalonarioValidacionRequest implements Serializable {

    private Integer codEmpresa;
    private String nroTalonario;
    private String reciboManual;
}
