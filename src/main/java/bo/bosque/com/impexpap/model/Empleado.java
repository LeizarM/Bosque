package bo.bosque.com.impexpap.model;
import java.io.Serializable;
import java.util.List;

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
    private String numCuenta;
    private int codRelBeneficios;
    private int codRelPlanilla;
    private int audUsuarioI;
    private int codDependiente;


    /**
     * Variables de apoyo
     */
    private Persona persona = new Persona();
    private EmpleadoCargo empleadoCargo = new EmpleadoCargo();
    private RelEmplEmpr relEmpEmpr = new RelEmplEmpr();
    private Dependiente dependiente= new Dependiente();

    private String esActivoString;

    private Telefono telefono= new Telefono();
    private Empresa empresa= new Empresa();
    private Sucursal sucursal= new Sucursal();
    private Email email= new Email();
    //private List<Formacion> formaciones;
    //private List <ExperienciaLaboral> experienciaLaboral;
    private Formacion formacion= new Formacion();
    private ExperienciaLaboral experienciaLaboral= new ExperienciaLaboral();
    private GaranteReferencia garanteReferencia= new GaranteReferencia();
    /**
     * Constructores
     */
    public Empleado(int codEmpleado, int codPersona, String numCuenta, int codRelBeneficios, int codRelPlanilla, int audUsuarioI) {


        this.codPersona = codPersona;
        this.numCuenta = numCuenta;
        this.codRelBeneficios = codRelBeneficios;
        this.codRelPlanilla = codRelPlanilla;
        this.audUsuarioI = audUsuarioI;
    }


}
