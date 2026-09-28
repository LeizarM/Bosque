package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Cuerpo de la peticion para resolver una propuesta de precios (aprobarla o
 * rechazarla) desde el circuito de autorizacion.
 *
 * <p>No lleva {@code audUsuario}: el usuario que resuelve sale del token, nunca
 * del cuerpo. Aprobar una propuesta cambia los precios de venta de toda la
 * empresa, asi que quien la aprueba tiene que ser quien esta autenticado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResolverPropuestaDto implements Serializable {

    /** Propuesta a resolver. Obligatorio. */
    private Long idPropuesta;

    /**
     * Estado destino, dominio de {@code tpr_autorizacion.esAprobada}
     * (vista {@code v_tipos}, grupo 38):
     * <ul>
     *   <li>{@code 0} Pendiente</li>
     *   <li>{@code 1} Aprobada — por familia, dispara la aplicacion de los precios
     *   propuestos; por articulo, solo la bitacora</li>
     *   <li>{@code 2} No Aprobada</li>
     *   <li>{@code 3} En Espera</li>
     * </ul>
     * Es {@code Integer} y no {@code Boolean}: la columna es tinyint con cuatro
     * valores en uso, y modelarla como booleano perderia informacion en el 30% de
     * las filas existentes.
     */
    private Integer esAprobada;
}
