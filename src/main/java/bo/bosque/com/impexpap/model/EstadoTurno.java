package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Catálogo de valores de celda del Rol de Turnos de Sábado — tabla {@code trs_EstadoTurno}.
 *
 * <p>SPs: {@code p_abm_trs_EstadoTurno} (I/U/D) · {@code p_list_trs_EstadoTurno} (L).
 *
 * <p>Los códigos son los del Excel original: 1 Trabaja · V Vacación · C Cambio ·
 * B Baja · X Feriado · L Libre · A Asueto cumpleaños · E Excusado del evento.
 * {@code cuentaTurno} es el único flag que leen los listados para sumar turnos.
 *
 * <p>Los campos que salen como <b>wrapper</b> ({@code Long} / {@code Integer}) son las
 * columnas que en BD admiten NULL: {@code BeanPropertyRowMapper} revienta si intenta
 * meter un NULL en un primitivo. En el ABM tambien conviene, porque asi un campo sin
 * cargar viaja como NULL y no como 0.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class EstadoTurno implements Serializable {

    // ── campos de la tabla trs_EstadoTurno ────────────────────────────────
    private int    idEstadoTurno;    // smallint, PK asignada a mano (no IDENTITY)
    private String codigoExcel;      // varchar(2), UNIQUE — la letra de la grilla
    private String nombre;           // varchar(40)
    private int    cuentaTurno;      // bit — 1 = suma al total de turnos (solo '1')
    private int    afectaCobertura;  // bit — 1 = deja un hueco ese día
    private int    requiereCambio;   // bit — 1 = exige fila en trs_Cambio (solo 'C')
    private String color;            // varchar(9), hex para pintar la grilla
    private String estado;           // char(1) — A=Activo, I=Inactivo
    private Long   audUsuario;

    // ── solo lectura (lo devuelve el SP, no es parámetro del ABM) ─────────
    private Date audFecha;
}
