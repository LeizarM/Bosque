package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor

public class Empleado implements Serializable {

    private int codEmpleado;
    private int codPersona;
    private long numCuenta;
    private int codRelBeneficios;
    private int codRelPlanilla;
    private int audUsuarioI;



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
