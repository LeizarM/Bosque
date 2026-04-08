package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ConfigComisionesBanco implements Serializable {

    private long idConfig;
    private int codBanco;
    private long idTipoTransaccion;
    private long idTipoCargo;
    private double valorPorcentaje;       // decimal(10,6) en BD
    private double valorFijo;             // decimal(18,2) en BD
    private long idMoneda;                // bigint en BD (era int)
    private int orden;
    private String baseCalculo;
    private int activo;
    private Date fechaVigenciaDesde;
    private Date fechaVigenciaHasta;
    private long audUsuario;              // bigint en BD (era int)

}
