package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BancoXCuenta implements Serializable {

    private int idBxC;
    private int codBanco;
    private String numCuenta;
    private String moneda;
    private int codEmpresa;
    private int audUsuario;

    private String nombreBanco;

}