package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una empresa del combo "Empresa" de la pantalla de cheques ({@code p_list_Empresa} 'C': columnas 1 y 2).
 * No es la empresa del usuario: el legacy la elige en la pantalla (arranca en la primera) y de ella salen las
 * sucursales, los clientes y la empresa del cheque que se registra.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaChequeDto implements Serializable {

    private int codEmpresa;
    private String nombre;
}
