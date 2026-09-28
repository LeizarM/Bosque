package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * Cuerpo de {@code /price/armado/agregarArticulos}: agrega articulos a una
 * propuesta por articulo (tipo 2), creandola si todavia no existe.
 *
 * <p>Reemplaza al boton "Agregar" de {@code dlgArtD}, que hacia un viaje por
 * articulo y creaba la propuesta con el primero. Aca viajan todos juntos y se
 * graban en una transaccion: si uno falla no queda ninguno.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArmadoArticulosDto implements Serializable {

    /** Nulo o 0 = alta; entonces son obligatorios {@link #titulo} y {@link #obs}. */
    private Long idPropuesta;

    private String titulo;
    private String obs;

    /** Codigos de tpr_articulo. Los que ya estan en la propuesta se omiten. */
    private List<String> codArticulos;
}
