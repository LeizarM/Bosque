package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Destruir documentos de una caja en un solo paso: las lineas elegidas o, con {@code lineas} vacia,
 * la caja completa. El SP lo guarda todo junto o nada. Los prestamos van con {@link PedidoEntrega}.
 */
@Getter
@Setter
@NoArgsConstructor
public class PedidoMovimientoCaja implements Serializable {

    private Long codCaja;
    private String observacion;
    private List<Long> lineas = new ArrayList<>();
}
