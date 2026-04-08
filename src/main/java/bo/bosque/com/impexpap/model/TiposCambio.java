package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TiposCambio implements Serializable {

    private long idTipoCambio;
    private int codBanco;
    private Date fechaVigencia;
    private long idMonedaOrigen;     // bigint en BD (era int)
    private long idMonedaDestino;    // bigint en BD (era int)
    private double tasaCompra;       // decimal(10,6) en BD
    private double tasaVenta;        // decimal(10,6) en BD
    private double tasaPromedio;     // decimal(10,6) en BD
    private String fuente;
    private long audUsuario;         // bigint en BD (era int)

    private Date fechaInicio;
    private Date fechaFin;

}
