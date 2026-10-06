package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una sucursal de una empresa, para el combo de la pantalla de cheques ({@code p_list_Sucursal} 'L': columnas 1 y 2). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SucursalChequeDto implements Serializable {

    private int codSucursal;
    private String nombre;
}
