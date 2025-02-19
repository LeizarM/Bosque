package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepositoCheque implements Serializable {

    private int idDeposito;
    private String codCliente;
    private int docNum;
    private int numFact;
    private int anioFact;
    private int codEmpresa;
    private int codBanco;
    private float importe;
    private String moneda;
    private int estado;
    private String fotoPath;
    private int audUsuario;

    private String nombreBanco;
    private String nombreEmpresa;
    private String fueReconciliado;


}