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
    private String nombre; //el nombre de la sucursal
    private int codEmpresa;
    private int codCiudad;
    private int audUsuarioI;

    /**
     * Variables de apoyo
     */
    private String nombreEmpresa;
    private String nombreCiudad;

    private Empresa empresa = new  Empresa();
}
