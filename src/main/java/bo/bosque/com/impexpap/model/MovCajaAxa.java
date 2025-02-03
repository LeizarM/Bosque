package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;



@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MovCajaAxa implements Serializable {

    private int idMcAxa;
    private String codigo;
    private Date fecha;
    private int estado;
    private double totalIngresosUSD;
    private double totalEgresosUSD;
    private double saldoFinalUSD;
    private double saldoInicialUSD;
    private int audUsuario;
    private double tc;
    private Date fechaActual;


    private String descripcion;
    private double montoUSD;

}
