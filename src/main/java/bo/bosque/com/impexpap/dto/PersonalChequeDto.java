package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una persona que puede dejar un cheque o recibirlo en custodia: fila de {@code p_list_Empleado}
 * ramas 'U' (jefe de cobranzas y cobradores) y 'C' (ademas, choferes), solo asignaciones activas de
 * la sucursal en {@code tb_asigFunModXEmpl} (modulo 41). Columnas 1 (codEmpleado) y 3 (nombre).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalChequeDto implements Serializable {

    private int codEmpleado;
    private String nombreCompleto;
}
