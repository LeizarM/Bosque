package bo.bosque.com.impexpap.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Un día del reporte de asistencia biométrica de un empleado.
 *
 * <p>Reemplaza al {@code WHILE} día-por-día de {@code p_Rpt_Biometrico}: se arma en
 * Java a partir de datos ya traídos en bloque (horarios, marcaciones del mes,
 * {@code IPermiso.diasNoHabiles}, {@code IPermiso.kardex}), sin una subquery por día.
 *
 * <p>{@link #estado} es la razón de ser de este DTO — es lo que faltaba en el reporte
 * legacy: distinguir una falta real de un día en el que el empleado no tenía por qué
 * marcar.
 */
@Getter
@Setter
public class AsistenciaDiaDto {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fecha;

    /**
     * TRABAJADO | FALTA | FERIADO | SABADO_LIBRE | PERMISO | VACACION | SIN_HORARIO.
     *
     * <p>{@code SIN_HORARIO}: el empleado no tenía ningún {@code tbio_bioHrEmpleado}
     * vigente ese día (p.ej. domingo, o antes de su primera asignación) — no es una
     * falta, es que ese día no estaba de turno según el horario cargado.
     */
    private String estado;

    private String motivo;

    private Date horaEntradaEsperada;
    private Date horaSalidaEsperada;
    private Date horaEntradaReal;
    private Date horaSalidaReal;

    /**
     * {@code true} si {@link #horaEntradaReal}/{@link #horaSalidaReal} vino de
     * {@code tbio_bioCHECKINOUTAdicinal} (una marcación olvidada, cargada a mano)
     * en vez de {@code tbio_bioCHECKINOUT} (el reloj biométrico). Antes de esto
     * esa tabla se guardaba pero nunca se leía de vuelta en el cálculo del
     * reporte — cargar una marcación olvidada no cambiaba nada en el día.
     */
    private boolean entradaManual;
    private boolean salidaManual;

    /** Ventana del permiso vigente ese día ({@code trh_permiso.desde/hasta}), con hora real — sólo si {@link #estado} es PERMISO o VACACION. */
    private Date horaInicioPermiso;
    private Date horaFinPermiso;

    /**
     * Minutos de atraso del día — nunca {@code null}: 0 en los días que no eran una obligación
     * real (feriado, sábado que no le tocaba, permiso, vacación, sin horario).
     *
     * <p><b>Fórmula reconstruida por ingeniería inversa, no leída de la SP</b> ({@code p_Rpt_Biometrico}
     * ACCION='C', columna {@code totalAtrasos} — SP de ~970 líneas sin ningún .sql local con su cuerpo,
     * y el acceso de sólo lectura para leerla vía {@code sp_helptext} quedó bloqueado esta sesión).
     * Confirmada contra ~15 casos reales de dos meses distintos, sin ninguna contradicción:
     * <ul>
     *   <li>Si falta la marcación de entrada O de salida (0 ó 1 marca ese día): atraso = duración
     *       completa del turno ese día — un "no marcó" completo, no un cálculo parcial. Confirmado
     *       con sentinelas de 510 (turno de 8.5h), 180 (sábado de 3h) y 600 (turno de 10h) — siempre
     *       igual a la duración exacta del turno del día, nunca un valor fijo genérico. Se calcula
     *       como {@code horaSalidaEsperada - horaEntradaEsperada} (las mismas Hr Ingreso/Hr Salida
     *       que el reporte muestra al lado), no como {@code tbio_bioHrs.cantMinutos}: ese campo
     *       suelto puede no coincidir con el ingreso/salida del propio turno — caso real que lo
     *       confirmó, un turno de 09:00 a 12:00 (180 min) con {@code cantMinutos=240} guardado
     *       aparte — y usarlo daba un número que no cuadraba con las horas de la misma fila.
     *       {@code cantMinutos} queda sólo de resguardo si algún turno no tuviera ingreso/salida.</li>
     *   <li>Si están las dos marcas: atraso = minutos de entrada tarde (si {@code > 10}, si no 0) +
     *       minutos de salida temprano (si {@code > 10}, si no 0) — cada pierna con su propia
     *       tolerancia de 10 minutos, sin restar la tolerancia del valor final (11 minutos tarde
     *       cuenta 11, no 1). Confirmado con casos de una sola pierna y de las dos violadas a la vez
     *       (p.ej. 111 + 128 = 239 exacto).</li>
     *   <li><b>Corrección deliberada sobre el legacy, pedida explícitamente por el usuario:</b> el
     *       legacy aplica esta fórmula incluso en sábados que el rol dice "no le toca" (siempre
     *       muestra el turno de 9 a 12 aunque nadie deba marcar), inflando el atraso con sentinelas
     *       de 180/510 en días que no eran una falta real. Acá el atraso se fuerza a 0 en cualquier
     *       día cuyo {@link #estado} no sea TRABAJADO ni FALTA.</li>
     * </ul>
     */
    private int minutosAtraso;

    public static final String TRABAJADO = "TRABAJADO";
    public static final String FALTA = "FALTA";
    public static final String FERIADO = "FERIADO";
    public static final String SABADO_LIBRE = "SABADO_LIBRE";
    public static final String PERMISO = "PERMISO";
    public static final String VACACION = "VACACION";
    public static final String SIN_HORARIO = "SIN_HORARIO";
}
