package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un empleado para elegir al responsable de un archivo o a quien se le presta ({@code p_list_tarc_Archivo} 'E'):
 * los activos y los que ya son responsables aunque hayan dejado de estarlo ({@code activo} = false).
 */
@Getter
@Setter
@NoArgsConstructor
public class EmpleadoArchivoDto implements Serializable {

    private long codEmpleado;
    private String nombreCompleto;
    private String cargo;
    private Boolean activo;
}
