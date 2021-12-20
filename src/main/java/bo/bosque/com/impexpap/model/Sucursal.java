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
public class Sucursal implements Serializable {

    private int codSucursal;
    private String nombre;
    private int codEmpresa;
    private int codCiudad;
    private int audUsuarioI;


    /**
     * Constructores
     */
    public Sucursal(int codSucursal, String nombre, int codEmpresa, int codCiudad, int audUsuarioI) {
        this.codSucursal = codSucursal;
        this.nombre = nombre;
        this.codEmpresa = codEmpresa;
        this.codCiudad = codCiudad;
        this.audUsuarioI = audUsuarioI;
    }
}
