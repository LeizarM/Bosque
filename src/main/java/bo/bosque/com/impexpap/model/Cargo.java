package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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

    private CargoSucursal cargoSucursal = new CargoSucursal();


    /*********************
     * *******Constructor
     ********************/
    public Cargo(int codCargo, int codCargoPadre, String descripcion, int codEmpresa, int codNivel, int posicion, int audUsuario) {
        this.codCargo = codCargo;
        this.codCargoPadre = codCargoPadre;
        this.descripcion = descripcion;
        this.codEmpresa = codEmpresa;
        this.codNivel = codNivel;
        this.posicion = posicion;
        this.audUsuario = audUsuario;
    }
}
