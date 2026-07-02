package bo.bosque.com.impexpap.model;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Date;

@Data
@NoArgsConstructor
public class SolicitudPermiso {
    // Parámetros de entrada para el SP p_abm_Solicitud
    private int codSolicitud;
    private int codEmpleado;
    private int codRelEmplEmpr;
    private String tipoPermiso;
    // Al usar Date, Jackson usa FlexibleDateDeserializer para aceptar el 'T' de Flutter,
    // y para serializar hacia SpHelper usará el formato global con ESPACIO.
    private Date desde;
    private Date hasta;
    private String horaInicio;      // ◄ NUEVO
    private String horaFin;         // ◄ NUEVO
    private String motivo;
    private float cantidadDias;
    private int estado;
    private Integer audUsuarioI; // Quien inserta, aprueba o rechaza

    // ── Auxiliares del SELECT de p_list_Solicitudes (acción 'P') ──────────
    private String nombreEmpleado;  // apPaterno + apMaterno + nombres
    private String cargoEmpleado;   // empCargo.cargoDescripcion
    private Date   fechaSolicitud;  // audFechaI de la solicitud
    private String pasoActual;      // "Esperando Jefe Inmediato" | "Esperando Autorización de RRHH"
    private int codUsuarioLogueado;
    private Integer codPermiso;     // ◄ Relación directa al permiso generado (si fue aprobado)
    // ── Auxiliares para previsualizar saldo (acción 'C') ──────────
    private Float diasSolicitados;
    private Float saldoRestante;
    private Float saldoActualBase;
    private String autorizador;
    private Float diasDisponibles;
    private String motivoRechazo;
}