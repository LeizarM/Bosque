package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DiaNoLaborableAdminSucursal implements Serializable {

    private long codSucursal;
    private String nombreSucEmpresa;

    /** 1 si la sucursal esta vinculada al dia no laborable consultado, 0 si no. */
    private int seleccionado;
}
