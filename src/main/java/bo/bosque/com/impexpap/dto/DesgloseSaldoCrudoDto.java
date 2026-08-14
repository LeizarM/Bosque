package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Proyección CRUDA de {@code p_list_Permiso @ACCION='D'} — las 20 columnas tal como salen del
 * SP, sin interpretar.
 *
 * <p><b>Este DTO no se publica.</b> Es el paso intermedio: el DAO lo lee y arma con él un
 * {@link DesgloseSaldoDto}, que es lo que viaja al cliente. Existe separado porque la
 * forma del SP (5 etiquetas y 5 montos como columnas sueltas, en dos bloques distintos del
 * SELECT) no es la forma que sirve para pintar una lista de 5 tramos.
 *
 * <p><b>Los 20 nombres son los alias del SP, calcados.</b> {@code BeanPropertyRowMapper} mapea
 * por NOMBRE. El DAO legacy leía las 16 primeras POR ÍNDICE: agregar una columna al SELECT le
 * corría todo. Acá reordenar el SELECT no rompe nada; renombrar un campo sí — y en silencio.
 *
 * <p><b>Ojo con las etiquetas: se pasan LITERALES, sin {@code trim()} ni reformateo.</b> Las
 * arma el propio SP con {@code UPDATE}s y traen rarezas que el criterio de aceptación #4 exige
 * carácter por carácter: un espacio antes del paréntesis de cierre ({@code "(28/02/2025 )"}),
 * DOS espacios después de {@code "cumplido"} en la segunda, y espacio final en la tercera y la
 * cuarta. Un {@code .trim()} bien intencionado rompe ese criterio.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DesgloseSaldoCrudoDto implements Serializable {

    // ── identificación ────────────────────────────────────────────────────
    private long   codEmpleado;
    private String datoEmpleado;

    // ── las 5 ETIQUETAS, en el orden en que el SP las emite ───────────────
    /** Tramo 1. {@code "Saldo hasta el penultimo año cumplido (28/02/2025 )"}. */
    private String datoDetSaldoPenUltAnio;
    /** Tramo 2. {@code "Vacación correspondiente a su 10º año cumplido  (01/03/2025 al 28/02/2026 )"}. */
    private String datoVacAsignadaSist;
    /** Tramo 3. {@code "Vacación acumulada desde su ultimo año cumplido (01/03/2026 ) hasta hoy "}. */
    private String datoVacAcumulad;
    /** Tramo 4. {@code "Vacación utilizada desde el (28/02/2025 ) hasta hoy "}. */
    private String datoVacUtilizada;
    /**
     * Tramo 5. {@code "Vacación programada a partir de hoy 11/08/2026"}.
     *
     * <p>Esta es la única de las cinco que el SP NO reescribe con un {@code UPDATE}: queda con
     * la fecha del {@code INSERT}, que es {@code getdate()} — o sea, siempre hoy. Correcta por
     * accidente, pero no depender de eso.
     */
    private String datoVacProgramada;

    // ── los 5 MONTOS ──────────────────────────────────────────────────────
    /** Tramo 1 · DEBE. Asignaciones hasta {@code fecIniPenUl} menos permisos VAC/PVA anteriores. */
    private double montoSaldoPenUltAnio;
    /**
     * Tramo 2 · DEBE.
     *
     * <p><b>SUPUESTO D2 — pendiente de confirmación de RR.HH. (ver plan §5).</b> Si
     * {@code (datoAnios - 1) <= 0} el SP cae en el {@code ELSE} y suma TODAS las asignaciones
     * históricas de la relación, no las del año: el número deja de significar lo que dice la
     * etiqueta —que además muestra {@code "-1º año cumplido"}—. Hoy le pasa a 57 de 85 activos.
     * Por eso {@link DesgloseSaldoDto#isDesgloseAplicable()} y por eso la UI no pinta los
     * 5 tramos cuando ese flag es false.
     */
    private double montoVacAsignadaSist;
    /**
     * Tramo 3 · INFO.
     *
     * <p><b>SUPUESTO D1 — pendiente de confirmación de RR.HH. (ver plan §5).</b> Es el único
     * tramo con duodécimas ({@code f_calcVacacionesAnioLaboral}) y el único que NO entra en el
     * total, igual que en el legacy. Se muestra desglosado y rotulado en vez de elegir por
     * RR.HH. cuál de los dos números es "el" saldo.
     */
    private double montoVacAcumulad;
    /** Tramo 4 · HABER. Permisos VAC/PVA con {@code desde} entre {@code fecIniPenUl} y hoy. */
    private double montoVacUtilizada;
    /** Tramo 5 · HABER. Permisos VAC/PVA con {@code desde > hoy}. */
    private double montoVacProgramada;

    // ── contexto del cálculo ──────────────────────────────────────────────
    /** {@code f_calcularAntiguedad}: años enteros. Es lo que decide {@code desgloseAplicable}. */
    private int    datoAnios;
    /**
     * String, no fecha: {@code "01/03/2015"} o el literal {@code "Sin Asignar"}. Ese literal
     * es lo que dispara {@link DesgloseSaldoDto#isTieneFechaBeneficio()}.
     *
     * <p>Se llama {@code datoFecIniBenef} —no {@code datoFechaBeneficio}, que es el nombre que
     * usa la ACCION 'C'— porque así se llama la columna en esta ACCION. Los dos SELECT del
     * mismo SP le pusieron nombres distintos al mismo dato; el mapeo es por nombre, así que
     * cada DTO usa el suyo.
     */
    private String datoFecIniBenef;
    /**
     * {@code fechaIniBeneficio + (datoAnios − 1) años − 1 día}.
     *
     * <p><b>Wrapper y no primitivo:</b> el SP sólo corre los dos {@code UPDATE} de fechas
     * {@code WHERE datoFecIniBenef != 'Sin Asignar'}, así que para un empleado sin fecha de
     * beneficio esta columna llega NULL. {@code BeanPropertyRowMapper} revienta si intenta
     * meter un NULL en un {@code long}.
     */
    private Date   fecIniPenUl;
    /** {@code fechaIniBeneficio + datoAnios años}. Mismo NULL posible que {@link #fecIniPenUl}. */
    private Date   fecIniUlt;
    private String datoAntiguedad;
    private String datoEmpresa;
    private String datoCargo;
    /** La relación laboral activa contra la que se calculó todo — ver D0 en {@link FichaSaldoDto}. */
    private long   codRelEmplEmpr;
}
