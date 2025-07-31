package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DepositoCheque implements Serializable {

    private int idDeposito;
    private String codCliente;
    private int codEmpresa;
    private int idBxC;
    private double importe;
    private String moneda;
    private int estado;
    private String fotoPath;
    private float aCuenta;
    private Date fechaI;
    private String nroTransaccion;
    private String obs;
    private int audUsuario;



    private int codBanco;
    private Date fechaInicio;
    private Date fechaFin;


    private String nombreBanco;
    private String nombreEmpresa;
    private String esPendiente;
    private String numeroDeDocumentos;
    private String fechasDeDepositos;
    private String numeroDeFacturas;
    private String totalMontos;

    private String estadoFiltro;


}