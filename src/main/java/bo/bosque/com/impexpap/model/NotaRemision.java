package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NotaRemision implements Serializable {

    private int idNR;
    private int idDeposito;
    private int docNum;
    private Date fecha;
    private int numFact;
    private float totalMonto;
    private float saldoPendiente;
    private int audUsuario;


    // Variables de apoyo
    private String codCliente;
    private String nombreCliente;
    private String db;
    private int codEmpresaBosque;
}