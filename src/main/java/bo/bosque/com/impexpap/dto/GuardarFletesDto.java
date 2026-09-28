package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * Cuerpo de {@code /price/armado/guardarFletes}: los fletes nuevos de una
 * propuesta por familia que ya existe.
 *
 * <p>Cambiar un flete cambia el precio de todas las listas de esa sucursal en
 * todas las familias de la propuesta, asi que la operacion recalcula todo en la
 * misma transaccion. Guardar solo el flete dejaria los precios propuestos
 * calculados con el valor viejo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuardarFletesDto implements Serializable {

    /** Propuesta por familia, pendiente. Obligatorio. */
    private Long idPropuesta;

    /** Se usan {@code codSucursal} y {@code valor}; el resto se ignora. */
    private List<CostoIncreSucursalDto> fletes;
}
