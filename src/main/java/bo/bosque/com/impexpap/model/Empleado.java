package bo.bosque.com.impexpap.model;
import java.io.Serializable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class Empleado extends Persona implements Serializable {

    private int codEmpleado;
    private int codPersona;
    private long numCuenta;
    private int codRelBeneficios;
    private int codRelPlanilla;
    private int audUsuarioI;


    /**
     * Variables de apoyo
     */
    Persona persona = new Persona();
    Cargo cargo  = new Cargo();
    RelEmplEmpr relEmpEmpr = new RelEmplEmpr();
    /**
     * Constructores
     */
    public Empleado(int codEmpleado, int codPersona, long numCuenta, int codRelBeneficios, int codRelPlanilla, int audUsuarioI) {

        this.codEmpleado = codEmpleado;
        this.codPersona = codPersona;
        this.numCuenta = numCuenta;
        this.codRelBeneficios = codRelBeneficios;
        this.codRelPlanilla = codRelPlanilla;
        this.audUsuarioI = audUsuarioI;
    }


}
