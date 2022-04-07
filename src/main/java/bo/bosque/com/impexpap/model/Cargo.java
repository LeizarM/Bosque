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
    private String descripcion; //cual es el cargo
    private int codEmpresa;
    private int codNivel;
    private int posicion;
    private int audUsuario;

    //variables de apoyo
    private String descripcionCargoPlanilla;
    private CargoSucursal cargoSucursal;

    private Empresa empresa;

}
