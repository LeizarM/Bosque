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
public class NoPagado implements Serializable {

    private long       idNoPagado;
    private Long       idPeriodo;
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
    private int        fuePagado;
    private int        fueCerrado;
    private int        esInterno;
    private int        ignoraComision;
    private Long       idGrupo;
    private String     grupo;
    private Date       fechaEntrada;
    private Date       fechaSalida;
    private Date       fechaUltimoPago;
    private int        diferenciaDias;
    private long       audUsuario;
    private Date       audFecha;

    private String siglaEmpresa;

}
