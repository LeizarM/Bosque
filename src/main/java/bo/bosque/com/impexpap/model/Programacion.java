package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * El evento de programar: un jefe decide otro horario para un dependiente —
 * tabla {@code trs_Programacion}.
 *
 * <p>SPs: {@code p_abm_trs_Programacion} (I/U/D) · {@code p_list_trs_Programacion} (L).
 *
 * <p>Guarda QUIÉN, CUÁNDO y con cuánta ANTELACIÓN. El alta delega en
 * {@code trs_sp_programar}, que valida el organigrama, crea la programación Y escribe
 * la celda en una sola transacción: crear la fila sola la dejaría sin efecto porque
 * nadie la apuntaría.
 *
 * <p><b>Una programación se REUSA:</b> si el mismo jefe programa a tres dependientes
 * para el mismo sábado, hay UNA fila y TRES celdas apuntándola. Por eso
 * {@code codEmpleadoDependiente} y {@code codigoExcel} son parámetros del ABM pero NO
 * columnas de la tabla: identifican a cuál celda aplicar esta programación.
 *
 * <p>{@code diasAntelacion} es DATO, no restricción: registra la anticipación, no la
 * impone. Nada bloquea si son 2 días — pero queda registrado.
 *
 * <p>El borrado ANULA ({@code estado='ANULADA'}) y libera las celdas: no borra, porque
 * las celdas 'P' apuntan aquí por FK.
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
public class Programacion implements Serializable {

    // ── parámetros del ABM ────────────────────────────────────────────────
    private long   idProgramacion;
    private long   idSabado;
    private long   codEmpleadoProgramador;  // el jefe, o su reemplazo
    private String estado;                  // VIGENTE | ANULADA
    private String motivo;                  // varchar(200)
    private Long   audUsuario;

    /** Sólo para {@code ACCION='I'}: a quién se le programa. No es columna de la tabla. */
    private long   codEmpleadoDependiente;
    /** Sólo para {@code ACCION='I'}: qué queda en su celda. 'L' = el jefe decide que no venga. */
    private String codigoExcel;

    // ── resto de la tabla ─────────────────────────────────────────────────
    private long   idRol;
    private Long   codEmpleadoEjecutor;     // si lo hizo el reemplazo; si no, NULL
    private Date   fechaProgramacion;       // cuándo se decidió
    private Integer diasAntelacion;          // DATEDIFF(día, fechaProgramacion, sábado)
    private Date   audFecha;

    // ── solo lectura, ACCION 'L' ──────────────────────────────────────────
    private Date   sabado;                  // trs_Sabado.fecha
    private String controlAntelacion;       // 'AVISO TARDIO (menos de 1 semana)' | 'ok'
    private int    celdasAfectadas;
}
