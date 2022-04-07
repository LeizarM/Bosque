package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class CargoSucursal {
    private int codCargoSucursal;
    private int codSucursal;
    private int codCargo;
    private int audUsuario;


    private Sucursal sucursal;
}
