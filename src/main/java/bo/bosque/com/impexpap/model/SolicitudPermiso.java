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
    // Con esta anotación, Jackson ignorará la 'T' y parsee la hora a Timestamp limpiamente
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Timestamp desde;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Timestamp hasta;
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