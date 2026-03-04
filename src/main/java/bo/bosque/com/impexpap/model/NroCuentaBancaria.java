package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor

public class NroCuentaBancaria implements Serializable {
    private int codCuenta;
    private int codEmpleado;
    private int codBanco;
    private String nroCuentaBancaria;
    private int estado;
    private int audUsuarioI;
}
