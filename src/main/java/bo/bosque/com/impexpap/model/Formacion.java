package bo.bosque.com.impexpap.model;

import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
public class Formacion {

    private int codFormacion;
    private int codEmpleado;
    private String descripcion;
    private int duracion;
    private String tipoDuracion;
    private String tipoFormacion;
    private Date fechaFormacion;
    private int audUsuario;
}
