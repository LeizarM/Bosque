package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una entrega a un empleado en un solo paso: cajas completas y documentos sueltos de cualquier caja.
 * El SP la guarda toda junta, con la misma hora, o nada.
 */
@Getter
@Setter
@NoArgsConstructor
public class PedidoEntrega implements Serializable {

    private Long codEmpleado;
    private String observacion;
    private List<Long> cajas = new ArrayList<>();
    private List<Long> lineas = new ArrayList<>();
}
