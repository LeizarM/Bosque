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
public class ParaPagar implements Serializable {

    private long       idParaPagar;
    private long       idPeriodo;
    private long       idVendedor;
    private int        codVendedor;
    private String     nombreVen;
    private int        codEmpresa;
    private int        docNum;
    private Date       fechaDoc;
    private String     valido;
    private String     indicador;
    private String     estado;
    private String     origen;
    private BigDecimal montoTotalUsd;
    private BigDecimal montoCerradoUsd;
    private BigDecimal montoTotalBs;
    private BigDecimal montoCerradoBs;
    private BigDecimal saldoPendiente;
    private BigDecimal tc;
    private BigDecimal comision;
    private BigDecimal porcentajeAplicado;  // congelado al calcular el periodo
    private int        fuePagado;
    private int        esInterno;
    private int        ignoraComision;
    private Long       idGrupo;
    private String     grupo;
    private Date       fechaInicio;
    private Date       fechaSalida;
    private Date       fechaUltimoPago;
    private int        diferenciaDias;
    private long       audUsuario;
    private Date       audFecha;

    // Agregados que devuelve el listado preliminar, no son columnas de la tabla.
    private BigDecimal ventaTotalMesUsd;
    private Integer    cantNotasPendientes;
    private String     siglaEmpresa;

}
