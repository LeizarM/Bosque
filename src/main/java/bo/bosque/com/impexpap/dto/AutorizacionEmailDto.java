package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.*;

/**
 * Forma del resultset de <b>p_list_autorizacion</b> en las ACCIONes 'B', 'C' y 'D'.
 * Las tres devuelven una sola columna {@code email} y por eso comparten DTO:
 *
 * <ul>
 *   <li><b>B</b>: correos del usuario indicado en {@code @audUsuario}.</li>
 *   <li><b>C</b>: correos del creador de la propuesta {@code @idPropuesta},
 *       filtrados al dominio &#64;impexpap.com.</li>
 *   <li><b>D</b>: correos de los empleados con la funcion AUTORIZADOR en el
 *       modulo 65 (precios), filtrados al dominio &#64;impexpap.com.</li>
 * </ul>
 *
 * <p>Es una forma incompatible con {@link AutorizacionDto}, por eso va en un DTO aparte.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AutorizacionEmailDto implements Serializable {

    /** Direccion de correo. Puede venir NULL en la ACCION 'D' si el autorizador no tiene correo IPX. */
    private String email;

}
