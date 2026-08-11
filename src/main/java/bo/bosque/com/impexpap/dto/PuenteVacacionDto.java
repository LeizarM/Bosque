package bo.bosque.com.impexpap.dto;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * El puente a cuenta de vacación de un sábado — entrada y simulación en un mismo DTO.
 *
 * <p>SP: {@code trs_sp_puenteVacacion} · {@code @ACCION='S'} simula y devuelve una fila
 * por persona; {@code @ACCION='A'} aplica y no devuelve resultset.
 *
 * <p><b>Qué es un puente.</b> La empresa declara que un sábado no se trabaja y se lo
 * cobra a la vacación de cada uno. Hasta ahora eran 42 permisos cargados a mano, uno por
 * persona, por la pantalla de solicitudes — que además los rebota: exige que no haya
 * cruces, pide dos firmas por solicitud y frena a quien tenga el saldo bajo. Ninguna de
 * esas tres cosas aplica cuando el que decide es la empresa.
 *
 * <p><b>Un solo DTO para las dos mitades</b>, igual que {@code Asignacion} en este mismo
 * módulo: arriba lo que se manda, abajo lo que devuelve la simulación. Separarlos daría
 * dos clases que hay que mantener sincronizadas para un endpoint que se llama dos veces.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PuenteVacacionDto implements Serializable {

    // ── entrada ───────────────────────────────────────────────────────────
    private long   idSabado;
    /**
     * Horario del permiso, {@code HH:mm}. Por defecto los pone el SP en
     * 08:30–12:30.
     *
     * <p><b>Cambiarlos cambia cuánto se le descuenta a cada uno</b>, y no de forma
     * obvia: {@code f_CalcularDiasHabilesPermiso} descuenta hasta 30 min de almuerzo a
     * toda franja que pise 12:00–14:00 y después topea el sábado en 240 min. 08:30–12:30
     * da 0.4375 días; 08:30–14:30 da 0.5, que es lo que usaron los 13 puentes anteriores.
     */
    private String horaDesde;
    private String horaHasta;
    private String motivo;

    // ── salida de @ACCION='S' (una fila por persona) ──────────────────────
    /** {@code ALTA} o {@code SE SALTEA}. */
    private String accion;
    private String nombreRol;
    private long   codEmpleado;
    /** Cuánto se le descontaría de vacación. Lo calcula el SP, no el cliente. */
    private double dias;
    /** La capa que escribió la celda: G rotación · P jefe · M manual. */
    private String origen;
    /** Por qué se saltea, cuando se saltea. Vacío si entra. */
    private String detalle;

    private Date   fecha;
    private Date   permisoDesde;
    private Date   permisoHasta;

    // ── totales: vienen repetidos en cada fila, como en el reporte del PDF ──
    private int    totalAlta;
    private int    totalSalteados;
    private double diasTotales;
}
