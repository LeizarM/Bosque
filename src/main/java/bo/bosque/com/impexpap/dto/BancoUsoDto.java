package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un banco con su uso, tal como lo devuelve {@code p_list_Banco} 'P' para la pantalla de bancos.
 * Los nombres siguen los alias del SELECT ({@code BeanPropertyRowMapper}).
 */
@Getter
@Setter
@NoArgsConstructor
public class BancoUsoDto implements Serializable {

    private int codBanco;
    private String nombre;
    private int cheques;
    private int cuentasEmpresa;
    /** Cuentas de sueldo vigentes de empleados ({@code tb_nroCuentaBancaria.estado = 1}). */
    private int cuentasEmpleados;
    private int movimientos;
    private int pagosExterior;
}
