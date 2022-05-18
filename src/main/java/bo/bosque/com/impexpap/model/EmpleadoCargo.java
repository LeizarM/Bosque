package bo.bosque.com.impexpap.model;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class EmpleadoCargo {

    private int codEmpleado;
    private int codCargoSucursal;
    private int codCargoSucPlanilla;
    private Date fechaInicio;
    private int audUsuario;

    private CargoSucursal cargoSucursal = new CargoSucursal();

    private String cargoPlanilla;
}
