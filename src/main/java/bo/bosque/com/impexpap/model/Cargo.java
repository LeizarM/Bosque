package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Cargo implements Serializable {

    private int codCargo;
    private int codCargoPadre;
    private String descripcion;
    private int codEmpresa;
    private int codNivel;
    private int posicion;
    private int audUsuario;

    //variables de apoyo
    private String descripcionCargoPlanilla;
    private CargoSucursal cargoSucursal = new CargoSucursal();

    private Sucursal sucursal = new Sucursal();

}
