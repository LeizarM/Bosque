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

    private static final long serialVersionUID = 1L;

    private int idDeposito;
    private String codCliente;
    private int codEmpresa;
    private int idBxC;
    /** Monto del depósito — usar BigDecimal para precisión monetaria */
    private BigDecimal importe;
    private String moneda;
    private int estado;
    private String fotoPath;
    /** Monto a cuenta — usar BigDecimal para precisión monetaria */
    private BigDecimal aCuenta;
    /** Fecha del depósito */
    private Date fechaI;
    private String nroTransaccion;
    private String obs;
    private long codEmpleado;
    private int audUsuario;

    /** Nombre de quien registró el depósito */
    private String nombreCompleto;
    /** Vendedor asociado a la nota/venta en SAP */
    private String nombreVendedor;

    private int codBanco;
    /** Filtro rango — fecha inicio */
    private Date fechaInicio;
    /** Filtro rango — fecha fin */
    private Date fechaFin;

    private String nombreBanco;
    private String nombreEmpresa;
    private String esPendiente;
    private String numeroDeDocumentos;
    private String fechasDeDepositos;
    private String numeroDeFacturas;
    private String totalMontos;

    /** Filtro de estado para la consulta reconciliada */
    private String estadoFiltro;

}
