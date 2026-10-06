// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\DependienteCargo.java
// Fila devuelta por p_list_tac_dependientesJefe — NO es una tabla propia,
// es la proyección de trh_cargo/tb_cargo_sucursal/tb_sucursal/trh_empleadoCargo
// que arma ese SP.
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DependienteCargo implements Serializable {

    private long codCargo;
    private String descripcionCargo;
    private int codNivel;
    private long codEmpresa;
    private Long codCargoSucursal;
    private long codSucursal;
    private String nombreSucursal;
    private int profundidadNivel;
    private Long codEmpleadoActual;
    private String nombreEmpleadoActual;
}
