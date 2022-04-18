package bo.bosque.com.impexpap.model;

import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class ExperienciaLaboral {

    private int codExperienciaLaboral;
    private int codEmpleado;
    private String nombreEmpresa;
    private String cargo;
    private String descripcion;
    private Date fechaInicio;
    private Date fechaFin;
    private String nroReferencia;
    private int audUsuario;
}
