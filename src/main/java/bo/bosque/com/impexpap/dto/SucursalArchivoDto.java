package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una sucursal para el combo del archivo fisico ({@code p_list_tarc_Archivo} 'S'). tb_sucursal se repite
 * por empresa (hay tres "Achocalla"): por eso viaja tambien el nombre de la empresa.
 */
@Getter
@Setter
@NoArgsConstructor
public class SucursalArchivoDto implements Serializable {

    private long codSucursal;
    private String nombre;
    private long codEmpresa;
    private String nombreEmpresa;
    private String ciudad;
}
