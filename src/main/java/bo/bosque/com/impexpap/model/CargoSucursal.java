package bo.bosque.com.impexpap.model;

import lombok.*;

@Data
@NoArgsConstructor
public class CargoSucursal {
    private int codCargoSucursal;
    private int codSucursal;
    private int codCargo;
    private int audUsuario;

    private String datoCargo;
    private Sucursal sucursal = new Sucursal();
    private Cargo cargo = new Cargo();

    //aqui tendria que estar el Cargo()
}
