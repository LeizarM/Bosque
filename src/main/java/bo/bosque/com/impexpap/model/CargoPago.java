package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CargoPago implements Serializable {

    private long idCargo;
    private long idCotizacion;
    private long idTransaccion;
    private long idTipoCargo;
    private double baseCalculo;    // decimal(18,2) en BD
    private String origenBase;
    private double porcentaje;     // decimal(10,6) en BD
    private double valorFijo;      // decimal(18,2) en BD
    private double montoCargo;     // decimal(18,2) en BD
    private long idMoneda;         // bigint en BD (era int)
    private int orden;
    private String descripcion;
    private long audUsuario;       // bigint en BD (era int)

}
