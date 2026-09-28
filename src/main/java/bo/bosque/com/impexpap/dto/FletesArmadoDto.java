package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Los fletes por sucursal para el armado de una propuesta por familia.
 *
 * <p>Si la propuesta ya existe son sus fletes guardados. Si es un alta son las
 * sucursales que cargaba {@code dlgNuevo} ({@code p_list_costoIncre 'C'}) con el
 * valor que tuvo cada una en la ultima propuesta por familia, para no volver a
 * tipearlos de memoria. El legacy arrancaba en cero y un flete olvidado en cero
 * abarataba toda la sucursal sin ningun aviso.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FletesArmadoDto implements Serializable {

    /**
     * De que propuesta salen los valores: la misma propuesta si ya existe, la
     * ultima por familia si es un alta, o nulo si no hubo ninguna de donde copiar.
     */
    private Long idPropuestaReferencia;

    /** true = son los fletes guardados de la propuesta; false = sugeridos. */
    private boolean deLaPropuesta;

    private List<CostoIncreSucursalDto> fletes = new ArrayList<>();
}
