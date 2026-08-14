package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Lo que devuelve {@code POST /permiso-rrhh/saldo/desglose}: la cabecera del empleado, los 5
 * tramos ya emparejados y los totales.
 *
 * <p><b>Es un OBJETO, no una lista</b> — se consume del lado Flutter con
 * {@code postAndReturnObject}. La ficha ({@code /saldo/ficha}) sí devuelve lista. La
 * diferencia importa: elegir mal el helper devuelve {@code null} en silencio, sin error.
 *
 * <p>El DAO lo arma a partir de {@link DesgloseSaldoCrudoDto} (las 20 columnas crudas de
 * {@code p_list_Permiso @ACCION='D'}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DesgloseSaldoDto implements Serializable {

    /** Etiqueta de la fila de total, igual que en el ERP legacy. */
    public static final String ETIQUETA_TOTAL = "TOTAL VACACION NO UTILIZADO";

    // ── cabecera ──────────────────────────────────────────────────────────
    private long   codEmpleado;
    private String datoEmpleado;
    private String datoEmpresa;
    private String datoCargo;
    private String datoAntiguedad;
    /** {@code "01/03/2015"} o el literal {@code "Sin Asignar"}. */
    private String datoFecIniBenef;
    /** Años enteros de antigüedad. Es lo que decide {@link #desgloseAplicable}. */
    private int    datoAnios;
    /** Puede venir {@code null}: ver {@link #tieneFechaBeneficio}. */
    private Date   fecIniPenUl;
    /** Puede venir {@code null}: ver {@link #tieneFechaBeneficio}. */
    private Date   fecIniUlt;
    /** La relación laboral activa contra la que se calculó todo (D0). */
    private long   codRelEmplEmpr;

    // ── banderas de estado ────────────────────────────────────────────────

    /**
     * false = {@code datoFecIniBenef == "Sin Asignar"}.
     *
     * <p><b>No significa "cinco ceros".</b> Sin fecha de beneficio el SP no corre los
     * {@code UPDATE} de fechas, así que sólo los tramos 1 y 4 dan 0: el 2 muestra TODA la
     * historia de la relación, el 3 las duodécimas y el 5 las programadas. Y las etiquetas
     * quedan con los defaults del {@code INSERT}, con fechas {@code 01/01/1900}. Por eso la
     * UI tiene que mostrar un banner de advertencia y no simplemente pintar los números.
     *
     * <p>Hoy 0 de 85 activos caen acá <i>(verificado)</i>, pero el camino existe.
     */
    private boolean tieneFechaBeneficio;

    /**
     * <b>SUPUESTO D2 — pendiente de confirmación de RR.HH. (ver plan §5).</b>
     * {@code datoAnios >= 2}.
     *
     * <p>Cuando es false —hoy 57 de 85 activos— el desglose en 5 tramos <b>no significa lo que
     * dice</b>: el SP cae en el {@code ELSE} del tramo 2 y suma toda la historia de la
     * relación en vez del año, y la etiqueta anuncia un {@code "-1º año cumplido"} que no
     * existe. <b>La UI no debe pintar los 5 tramos con este flag en false</b>; muestra el
     * estado explícito acordado en el acta más los tres números que sí son fiables (asignados,
     * utilizados, programados).
     *
     * <p>Se manda como bandera y no se resuelve escondiendo tramos del lado del servidor para
     * que la decisión de qué mostrar quede en un solo lugar —la pantalla— y sea revisable.
     */
    private boolean desgloseAplicable;

    // ── los 5 tramos ──────────────────────────────────────────────────────
    /**
     * Siempre los 5, en orden. Arranca en lista vacía y no en {@code null} para que el cliente
     * no tenga que preguntar antes de recorrerla.
     */
    private List<TramoSaldoDto> tramos = new ArrayList<>();

    // ── totales (réplica exacta de PermisoManagedBean:724-726) ────────────

    /** {@code montoSaldoPenUltAnio + montoVacAsignadaSist}. */
    private double montoTotalDebe;
    /** {@code montoVacUtilizada + montoVacProgramada}. */
    private double montoTotalHaber;
    /**
     * {@code montoTotalDebe − montoTotalHaber}.
     *
     * <p><b>SUPUESTO D1 — pendiente de confirmación de RR.HH. (ver plan §5).</b> El tramo 3
     * ({@code ACUMULADA}, las duodécimas del año en curso) <b>NO entra en este total</b>,
     * exactamente como en el legacy. Se muestran los dos números desglosados y rotulados en
     * vez de elegir uno: el tramo 3 viaja con {@code signo = INFO} para que quede dicho en el
     * contrato y no sólo en este comentario.
     *
     * <p>Puede ser negativo, y se muestra negativo: significa que el empleado tomó más días de
     * los que tenía. Es un dato real del ERP, no un error de cálculo.
     */
    private double montoTotal;
    /** {@link #montoTotal} formateado — ver {@code Fmt.dias}. */
    private String montoTotalTxt;
    /** Siempre {@link #ETIQUETA_TOTAL}. Viaja para que el cliente no tenga que hardcodearla. */
    private String etiquetaTotal;
}
