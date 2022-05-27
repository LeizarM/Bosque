package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
@AllArgsConstructor
public class Cargo implements Serializable {

    private int codCargo;
    private int codCargoPadre;
    private String descripcion; //cual es el cargo
    private int codEmpresa;
    private int codNivel;
    private int posicion;
    private int audUsuario;

    private String sucursal;
    private String sucursalPlanilla;
    private String nombreEmpresa;
    private String nombreEmpresaPlanilla;
    private int codEmpresaPlanilla;
    private int codCargoPlanilla;
    private String descripcionPlanilla;

}
