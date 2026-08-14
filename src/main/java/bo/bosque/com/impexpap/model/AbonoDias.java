package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Una fila de {@code trh_abonoDias}: días libres que se le regalan a alguien fuera del ciclo de
 * vacación (compensaciones, regularizaciones). También suma al "debe" del saldo.
 *
 * <p><b>Modelo y no DTO</b> por lo mismo que {@link VacacionAsignada}: es la forma de la tabla.
 *
 * <h3>Ojo con el orden de los parámetros del SP</h3>
 * En {@code p_abm_AbonoDias} la {@code @fecha} va ANTES que el {@code @motivo}, al revés que en
 * {@code p_abm_vacacionAsignada}. Los DAO mandan por nombre justamente por eso.
 *
 * <h3>Qué escribe cada ACCION del SP</h3>
 * <ul>
 *   <li>{@code 'I'} — inserta todo menos el id y {@code audFechaI} ({@code GETDATE()}).</li>
 *   <li>{@code 'U'} — a diferencia de vacación asignada, <b>SÍ pisa {@code codEmpleado} y
 *       {@code codRelEmplEmpr}</b>. Como {@code codEmpleado} es NOT NULL, una 'U' que no los
 *       mande revienta el UPDATE entero: la edición manda siempre la fila completa, releída de
 *       la base, y sólo deja cambiar días y motivo.</li>
 *   <li>{@code 'D'} — DELETE físico, archivado por el trigger {@code dad_abonoDias} en
 *       {@code trh_abonoDiasEliminado}.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
public class AbonoDias {

    /** IDENTITY. {@code 0} en un alta. */
    private long codAbonoDias;
    private long codEmpleado;
    /** {@code float} de SQL. Acá el cero NO es válido: el mínimo real medido es 0,5. */
    private double diasAbonados;
    /** {@code date}. En la edición no se toca (el legacy deshabilita el calendario). */
    private Date fecha;
    /** {@code varchar(50)} — nullable en la tabla, obligatorio por regla de negocio. */
    private String motivo;
    /** Nullable en la tabla, por eso {@code Long} y no {@code long}. */
    private Long codRelEmplEmpr;
    private long audUsuarioI;
    private Date audFechaI;

    // ── auxiliares ────────────────────────────────────────────────────────
    /** Apellidos + nombres. Lo resuelve el DAO para la simulación de la carga grupal. */
    private String datoEmpleado;
    /** {@code diasAbonados} ya formateado por {@code Fmt.dias}. */
    private String diasAbonadosTxt;
    /**
     * Simulación de la carga grupal: si esta persona entra o no. Es el equivalente al dry-run de
     * {@code trs_sp_puenteVacacion}, que también devuelve una fila por persona con su motivo.
     */
    private boolean aplica;
    /** Por qué queda afuera. Vacío cuando {@link #aplica} es {@code true}. */
    private String motivoOmision;
}
