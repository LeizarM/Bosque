package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * DTO exclusivo para el endpoint /confirmar-pago.
 * Agrupa datos de Transacciones + SolicitudPago en un solo body
 * porque ambas entidades se cierran en la misma TX Java (ACID).
 */
@Getter
@Setter
public class ConfirmarPagoRequest {

    private long   idTransaccion;        // primitivo: evita NPE al unboxing (Long → long)
    private long   idSolicitud;          // primitivo: evita NPE al unboxing
    private String numeroTransaccion;
    private Date   fechaValor;

    private long  audUsuario;            // bigint en BD
    private int   codEmpresa;
    private double montoTotalSolicitud;  // decimal(18,2) en BD — float pierde precisión
    private Date   fechaSolicitud;       // NOT NULL en BD — necesario para UPDATE completo

}