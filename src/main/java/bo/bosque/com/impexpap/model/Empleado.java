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

    /**
     * Constructores
     */
    public Empleado(int codEmpleado, int codPersona, String numCuenta, int codRelBeneficios, int codRelPlanilla, int audUsuarioI) {

        this.codEmpleado = codEmpleado;
        this.codPersona = codPersona;
        this.numCuenta = numCuenta;
        this.codRelBeneficios = codRelBeneficios;
        this.codRelPlanilla = codRelPlanilla;
        this.audUsuarioI = audUsuarioI;
    }


}
