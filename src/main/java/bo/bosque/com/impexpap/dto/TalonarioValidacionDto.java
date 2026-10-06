package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * El resultado de comprobar un par talonario/recibo antes de guardar. Es un <b>aviso temprano</b>: el servidor vuelve a
 * validar al guardar con la misma regla.
 *
 * <ul>
 *   <li>{@code valido = true}: el par es correcto, o no hay nada que comprobar contra la base (talonario o recibo vacios o
 *       en 0). {@code detalle} dice a que talonario y rango pertenece, cuando se conoce.</li>
 *   <li>{@code valido = false}: {@code mensaje} explica por que, una o varias lineas separadas por {@code \n}.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TalonarioValidacionDto implements Serializable {

    private boolean valido;
    private String mensaje;
    private String detalle;
}
