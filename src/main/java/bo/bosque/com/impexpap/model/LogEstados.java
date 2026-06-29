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
    private Long   idSolicitudProveedor;  // bitácora granular: cambios de estado por proveedor
    private Long   idDetalle;             // bitácora granular: aprobación por cuota
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

    /** Nombre completo del usuario (audUsuario) resuelto vía tb_usuario→tb_empleado→trh_persona.
     *  Fallback en el SP: nombre completo → login → 'Usuario #<id>'. */
    private String nombreUsuario;

}
