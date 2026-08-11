package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * El corazón del módulo: la matriz — tabla {@code trs_Asignacion}.
 *
 * <p>SPs: {@code p_abm_trs_Asignacion} (I/U/D) · {@code p_list_trs_Asignacion} (L, M, A).
 *
 * <p>Una fila = una casilla de la grilla ({@code participante × sábado → estado}).
 * <b>LIBRE NO SE GUARDA:</b> si a alguien no le toca ese sábado, no existe la fila.
 * Por eso 12 personas × 13 sábados no dan 156 filas sino 87. Escribir 'L' por el ABM
 * BORRA la fila.
 *
 * <p>Las tres capas de {@code origen} son lo que permite regenerar sin perder trabajo
 * humano — el DELETE del generador filtra por {@code origen='G'}:
 * <ul>
 *   <li>{@code G} — la escribe el generador; se borra y se rehace</li>
 *   <li>{@code P} — {@code trs_sp_programar}; sobrevive</li>
 *   <li>{@code M} — {@code trs_sp_corregirCelda}; sobrevive</li>
 * </ul>
 *
 * <p><b>OJO: {@code origen} no dice si un humano intervino.</b> Las celdas del evento y
 * de la convocatoria también son 'G', porque se DERIVAN de {@code trs_Sabado.alcanceEvento}
 * y {@code trs_Convocatoria} y tienen que rehacerse en cada regeneración. Para saber
 * cuánto resolvió el sistema solo, usar {@code ACCION='A'}.
 *
 * <p>Prioridad del valor: FERIADO 'X' &gt; EXCUSADO 'E' &gt; EVENTO '1' &gt; ASUETO 'A' &gt; TRABAJA '1'.
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
public class Asignacion implements Serializable {

    // ── parámetros del ABM ────────────────────────────────────────────────
    private long   idAsignacion;
    private long   idParticipante;
    private long   idSabado;
    /** La letra destino. 'L' o vacío = dejar LIBRE, que BORRA la fila. */
    private String codigoExcel;
    private String observacion;      // varchar(200) — alineado con trs_Convocatoria.motivo
    private Long   audUsuario;

    // ── quién escribe: lo pone el CONTROLADOR desde el token ──────────────
    /*
     * NUNCA se leen del body. El controlador los pisa con los del token antes de
     * llamar al DAO, igual que /programar hace con codEmpleadoProgramador. Si se
     * aceptaran del cliente, mentir sobre quién sos sería cambiar un número en el
     * JSON y todo el control del SP no valdría nada.
     *
     * Viajan al SP porque p_abm_trs_Asignacion los declara: con esAdmin=0 y un
     * codEmpleadoEjecutor que no es de RR.HH. ni jefe del participante, el SP
     * corta con el error 29.
     */
    private Long   codEmpleadoEjecutor;
    /** 1 si el token trae ROLE_ADM. {@code int} y no {@code boolean} para no depender de cómo Jackson serializa un BIT. */
    private int    esAdmin;

    // ── resto de la tabla ─────────────────────────────────────────────────
    private long   idRol;
    private int    idEstadoTurno;
    private String origen;           // char(1): G | M | P
    private Long   idProgramacion;   // sólo cuando origen='P'
    private Date   audFecha;

    // ── solo lectura, ACCION 'L': joins ───────────────────────────────────
    private String nombreRol;
    private String grupoRotacion;
    private Date   fecha;            // trs_Sabado.fecha
    private String estadoNombre;     // trs_EstadoTurno.nombre

    // ── solo lectura, ACCION 'L': con quién se hizo el cambio ─────────────
    /*
     * Salen de trs_Cambio, no de trs_Asignacion. La celda sola nunca supo con
     * quién: una 'C' dice "no vino y lo cubrieron" sin decir quién, y el '1'
     * del que cubrió es idéntico a un sábado que le tocaba por rotación.
     *
     * Vienen vacíos —no null— cuando no hay cambio detrás, que es el caso de
     * casi todas las celdas.
     *
     * OJO con agregarles valor desde el cliente: en las ESCRITURAS el modelo
     * entero viaja al SP. Hoy no rompe porque /corregir-celda pasa por
     * ejecutarAbm(), que usa SimpleJdbcCall y descarta los parámetros que el SP
     * no declara. Si alguna vez esa ruta cambiara a ejecutarAbmMap(), que
     * concatena las claves tal cual, un @cambioCon=? haría fallar el SP.
     */
    /** El otro, ya resuelto a nombre (trs_Participante.nombreRol). */
    private String cambioCon;
    /** 'T' = esta persona falta y la cubren · 'R' = esta persona cubre a otro. */
    private String cambioRol;
    /** COBERTURA | PERMUTA. */
    private String cambioTipo;

    // ── solo lectura, ACCION 'M' (mezcla por capa y estado) ───────────────
    private String queEs;            // texto legible del origen
    private String estado;           // el codigoExcel agrupado
    private String nombre;           // trs_EstadoTurno.nombre
    private int    celdas;

    // ── solo lectura, ACCION 'A' (cuánto resuelve el sistema solo) ────────
    // Los dos SUM son wrapper: sobre un rol sin celdas, SUM() devuelve NULL
    // (COUNT devuelve 0, SUM no) y un int primitivo reventaría al mapear.
    private int     celdasTotales;
    private Integer conDecisionHumana;
    private Integer generadasSolas;
}
