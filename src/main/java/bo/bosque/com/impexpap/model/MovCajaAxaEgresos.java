package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MovCajaAxaEgresos implements Serializable {

    private int idEg;
    private int idMcAxa;
    private String descripcion;
    private double montoBS;
    private double montoUSD;
    private int docNum;
    private long numAsiento;
    private double acumuladoUSD;
    private double acumuladoBS;
    private double tc;
    private int idEgRec;
    private int codUsuarioRevisado;
    private Date fechaDeclaracion;
    private Date fechaDocNum;
    private int estado;
    private String tipoTransaccion;
    private String origen;
    private int audUsuario;

    private String fechaDeclaracionCad;
    private String fechaDocNumCad;
    private String montoUSDDocNum;
    private String tipo;
    private int fila;
}
