package bo.bosque.com.impexpap.model;
import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Empleado implements Serializable {

    private int codEmpleado;
    private int codPersona;
    private String numCuenta;
    private int codRelBeneficios;
    private int codRelPlanilla;
    private int audUsuarioI;


    /**
     * Variables de apoyo
     */
    Persona persona = new Persona();
    Cargo cargo  = new Cargo();
    RelEmplEmpr relEmpEmpr =  new RelEmplEmpr();


}
