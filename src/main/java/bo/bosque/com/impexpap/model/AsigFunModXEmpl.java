package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Tabla {@code tb_asigFunModXEmpl}: una función de un módulo asignada a un empleado en una sucursal.
 * Los nombres (función, módulo, empleado, sucursal, empresa) solo los llena {@code p_list_AsigFunModXEmpl 'P'}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AsigFunModXEmpl implements Serializable {

    private long codAsignacion;
    private long codFuncMod;
    private String funcion;
    private long codModulo;
    private String modulo;
    private long codEmpleado;
    private String empleado;
    private long codSucursal;
    private String sucursal;
    private String empresa;
    /** 1 activo, 0 inactivo. */
    private String estado;
    private long audUsuario;
}
