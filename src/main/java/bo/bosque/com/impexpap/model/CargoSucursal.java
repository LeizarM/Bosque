package bo.bosque.com.impexpap.model;

import lombok.*;

@Data
@NoArgsConstructor
public class CargoSucursal {
    private int codCargoSucursal;
    private int codSucursal;
    private int codCargo;
    private int audUsuario;


    private Sucursal sucursal = new Sucursal();
    private String datoCargo;
}
