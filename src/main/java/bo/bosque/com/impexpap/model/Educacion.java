package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Educacion implements Serializable {
    private int codEducacion;
    private int codEmpleado;
    private String tipoEducacion;
    private String descripcion;
    private Date fecha;
    private int audUsuario;
}
