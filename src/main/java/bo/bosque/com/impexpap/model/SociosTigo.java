package bo.bosque.com.impexpap.model;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SociosTigo {
    private int codCuenta;
    private int telefono;
    private Integer codEmpleado;
    private String nombreCompleto;
    private String descripcion;
    private int audUsuario;

    private String periodoCobrado;

}
