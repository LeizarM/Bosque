package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;



@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MovCajaAxaIngresos implements Serializable {


    private int idIng;
    private int idMcAxa;
    private String descripcion;
    private double montoBS;
    private double montoUSD;
    private int docNum;
    private long numAsiento;
    private double acumuladoUSD;
    private double acumuladoBS;
    private double tc;
    private int idIngRec;
    private int codUsuarioRevisado;
    private Date fechaDeclaracion;
    private Date fechaDocNum;
    private int estado;
    private String tipoTransaccion;
    private String origen;
    private int audUsuario;

    private String fechaDeclaracionCad;
    private String fechaDocNumCad;
    private String tipo;
    private String montoUSDDocNum;
    private int fila;
    private String fechaInicio;
    private String fechaFin;

    private double debito; //debito del sap productivo en USD
    private double credito; // credito del sap productivo en USD
    private double saldoInicial;
    private double saldoAcumulado;



}
