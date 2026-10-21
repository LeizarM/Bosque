package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Una fila de la agenda del Dashboard ({@code p_list_RelEmplEmpr 'G'}): un empleado con relación
 * vigente, su cargo y sucursal actuales y sus teléfonos corporativos separados por coma.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AgendaEmpleadoDto implements Serializable {

    private long codEmpleado;
    private String nombreCompleto;
    private String cargo;
    private long codSucursal;
    private String sucursal;
    private String empresa;
    private String telefonos;
}
