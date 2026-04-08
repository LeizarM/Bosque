package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Cotizaciones implements Serializable {

    private long idCotizacion;
    private long idSolicitud;
    private Date fechaCotizacion;
    private double montoCompra;           // decimal(18,2) en BD
    private long idMoneda;                // bigint en BD (era int — truncaba)
    private int nroGiros = 1;             // BD: default 1 NOT NULL — Java int default es 0, que viola la restricción
    private int codBanco;
    private double tipoCambioOfrecido;    // decimal(10,6) en BD
    private double montoConvertido;       // decimal(18,2) en BD
    private double totalBolivianos;       // decimal(18,2) en BD
    private int esGanadora;
    private String estado;
    private String observaciones;
    private long audUsuario;              // bigint en BD (era int)

    private Date fechaInicio;
    private Date fechaFin;

    List<CargoPago> cargos;

}
