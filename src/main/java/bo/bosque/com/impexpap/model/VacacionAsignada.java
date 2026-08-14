package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Una fila de {@code trh_vacacionAsignada}: los días que la empresa le RECONOCE a alguien por
 * haber cumplido un año más de relación laboral. Es el "debe" del saldo.
 *
 * <p><b>Modelo y no DTO</b> porque es la forma de la tabla, columna por columna, incluida la
 * auditoría. Lo calculado (la ficha, el desglose en tramos) sigue viviendo en {@code dto/}.
 *
 * <h3>Nunca como filtro de {@code SpHelper.ejecutarListado}</h3>
 * El overload de MODELO serializa TODOS los campos a {@code @campo=?}, y los cuatro auxiliares de
 * acá abajo no son parámetros de {@code p_list_vacacionAsignada}: el {@code EXEC} fallaría con
 * "too many arguments". Los DAO de este módulo mandan siempre parámetros explícitos.
 *
 * <h3>Qué escribe cada ACCION del SP</h3>
 * <ul>
 *   <li>{@code 'I'} — inserta todo menos el id (es IDENTITY) y {@code audFechaI} (lo pisa con
 *       {@code GETDATE()} aunque declare el parámetro).</li>
 *   <li>{@code 'U'} — actualiza SÓLO {@code diasAsignados}, {@code motivo}, {@code fecha} y la
 *       auditoría. <b>{@code codEmpleado} ni siquiera está en el SET y {@code codRelEmplEmpr}
 *       está comentado a propósito</b>: mover la fila de relación le cambiaría el saldo a dos
 *       personas-relación a la vez.</li>
 *   <li>{@code 'D'} — DELETE físico. El trigger {@code dad_vacacionAsignada} archiva la fila en
 *       {@code trh_vacacionAsignadaEliminado} antes de que desaparezca, así que Sistemas la puede
 *       recuperar.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
public class VacacionAsignada {

    /** IDENTITY. {@code 0} en un alta y en las filas sintéticas que inventa la ACCION 'B'. */
    private long codVacacionAsignada;
    private long codEmpleado;
    /** {@code float} de SQL. <b>Cero es un valor válido</b>: 446 de las 1.424 filas lo usan. */
    private double diasAsignados;
    /** {@code varchar(300)} NOT NULL. */
    private String motivo;
    /** {@code date}. La pone el servidor: es el aniversario de la relación, no un campo libre. */
    private Date fecha;
    /** Nullable en la tabla, por eso {@code Long} y no {@code long}. */
    private Long codRelEmplEmpr;
    private long audUsuarioI;
    private Date audFechaI;

    // ── auxiliares ────────────────────────────────────────────────────────
    /** Sólo lo devuelve la ACCION 'B' (y viene vacío: el SP lo selecciona como {@code ''}). */
    private String datoEmpleado;
    /** Ídem {@link #datoEmpleado}. */
    private String datoRelacion;
    /** {@code diasAsignados} ya formateado por {@code Fmt.dias} para que la app no redondee. */
    private String diasAsignadosTxt;
    /**
     * {@code true} cuando la fila NO existe todavía: es un aniversario que la ACCION 'B' inventa
     * con {@code codVacacionAsignada = 0} para que la grilla ofrezca "Nuevo" en vez de "Editar".
     */
    private boolean sintetico;
}
