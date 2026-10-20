package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Lote 'E'} (contrato 14): un empleado con relación laboral vigente, su CI y su cargo
 * actual. Si no se le puede asignar un examen, {@code puedeRendir} es falso y {@code motivo} dice por qué.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeEmpleadoElegibleDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codEmpleado;

    private String nombreCompleto;

    private String ci;

    private String cargo;

    private Boolean puedeRendir;

    private String motivo;
}
