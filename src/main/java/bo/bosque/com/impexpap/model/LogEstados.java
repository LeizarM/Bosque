package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class LogEstados implements Serializable {

    // ── Campos de la tabla tpex_LogEstados ────────────────────────────────
    private long   idLog;           // PK — siempre NOT NULL
    // Exactamente uno de los tres siguientes tiene valor; los otros son NULL en BD.
    // Deben ser Long (wrapper) para que BeanPropertyRowMapper pueda asignar NULL.
    private Long   idSolicitud;
    private Long   idCotizacion;
    private Long   idTransaccion;
    private String estadoAnterior;
    private String estadoNuevo;
    private String observaciones;
    private long   audUsuario;      // bigint en BD — siempre NOT NULL
    private Date   audFecha;        // timestamp del cambio de estado

    // ── Campos derivados de los SELECTs del SP ────────────────────────────
    /** 'SOLICITUD' | 'COTIZACION' | 'TRANSACCION' — calculado con CASE en el SP */
    private String tipoEntidad;

    /** ID de la entidad afectada como VARCHAR (usado en acción B y F) */
    private String idEntidad;

}
