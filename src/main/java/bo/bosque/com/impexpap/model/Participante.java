package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Foto del organigrama dentro del rol — tabla {@code trs_Participante}.
 *
 * <p>SPs: {@code p_abm_trs_Participante} (I/U/D) · {@code p_list_trs_Participante} (L, T, K).
 *
 * <p>Cargo, nivel y fecha de nacimiento se CONGELAN al generar, para que el rol no
 * cambie si mañana la persona rota de cargo. {@code nroOrden} sale de ordenar por
 * (sucursal, nivel, posición, nombre) y de su paridad sale {@code grupoRotacion}:
 * impar → A, par → B.
 *
 * <p><b>{@code codSucursal} decide qué feriado le toca a cada uno.</b> Los feriados
 * son por sucursal ({@code trh_diaNoLaborable_sucursal}) y hay departamentales que
 * aplican a una sola: si cae sábado, quedan en 'X' sólo los de esa sucursal y los
 * demás trabajan igual. A diferencia del cargo, esta columna SÍ se refresca al
 * regenerar.
 *
 * <p><b>PENDIENTE:</b> {@code codSucursal} es columna de la tabla pero HOY NO es
 * parámetro de {@code p_abm_trs_Participante}. Un alta por ABM la deja en NULL y a
 * esa persona no le aplicaría ningún feriado. Hay que agregar el parámetro al SP.
 *
 * <p><b>Nunca se borra una fila</b>, para conservar el historial. Pero "no está" son
 * DOS hechos distintos, cada uno con su columna y su único dueño — antes compartían
 * el bit {@code activo} y se pisaban:
 * <pre>
 *   activo               -> sigue o no en la relación laboral vigente.
 *                           Lo escribe SÓLO trs_sp_generarRol @modo='REGENERAR'.
 *   fechaAlta/fechaBaja  -> desde/hasta cuándo hace sábados.
 *                           Lo escribe SÓLO RR.HH. ('R' / 'D'); el generador ni las mira.
 * </pre>
 * La ACCION 'L' resuelve las cuatro combinaciones en {@link #situacion}.
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
public class Participante implements Serializable {

    // ── parámetros del ABM ────────────────────────────────────────────────
    private long   idParticipante;
    private long   idRol;
    private long   codEmpleado;
    private Long   codGerente;        // jefe directo; trs_sp_generarRol lo deja NULL
    private Long   codCargo;          // foto del cargo vigente
    private Long   codCargoSucursal;  // foto de tb_cargo_sucursal
    private Integer codNivel;          // foto del nivel de organigrama
    private Date   fechaNacimiento;   // foto, para el asueto
    private Integer nroOrden;
    private String nombreRol;         // apPaterno + ' ' + nombres
    private String grupoRotacion;     // char(1): 'A' | 'B'
    private Integer turnosObjetivo;    // meta individual; gana sobre la del rol

    /**
     * <b>La ventana de participación en los sábados de ESTE rol:</b> desde y hasta
     * cuándo la persona entra en la rotación. NULL de los dos lados = ventana
     * abierta, participa todo el año — que es el caso de casi todos.
     *
     * <p>Las escribe RR.HH. por {@code @ACCION='D'} (sale, {@link
     * IParticipante#sacarDeSabados}) y {@code @ACCION='R'} (vuelve, {@link
     * IParticipante#reincorporar}), y {@code trs_sp_generarRol} NUNCA las toca.
     * Ahí está el reparto que hace que una baja no se deshaga sola en la próxima
     * regeneración.
     *
     * <p>La ventana es POR ROL: sacar a alguien del ROL 2026 no lo saca del 2027
     * — eso lo hereda {@code trs_sp_generarRol} al crear la gestión siguiente.
     */
    private Date fechaAlta;
    private Date fechaBaja;

    /**
     * <b>Responde una sola pregunta: si sigue en la relación laboral vigente.</b>
     * Su ÚNICO escritor es {@code trs_sp_generarRol @modo='REGENERAR'}, que lo
     * reconcilia contra {@code tb_relEmplEmpr} en cada corrida: lo prende al que
     * reaparece y lo apaga al que ya no está.
     *
     * <p><b>Ya NO significa "RR.HH. lo sacó de los sábados".</b> Escribirlo a mano
     * no servía —la siguiente regeneración lo deshacía sin preguntar por qué estaba
     * en 0—, así que esa decisión se mudó a {@code fechaAlta}/{@code fechaBaja} y el
     * {@code 'U'} del ABM IGNORA este campo a propósito.
     *
     * <p>BD: {@code bit NOT NULL DEFAULT 1}. Inicializado en 1 a propósito: con el 0
     * de Java, un {@code U} parcial daría de baja al participante sin querer.
     */
    private int activo = 1;

    private Long audUsuario;

    // ── columnas de la tabla que el ABM todavía NO expone ─────────────────
    /** Ver el javadoc de la clase: falta agregarla como parámetro del SP. */
    private Long codSucursal;
    /** Atajo de trs_Programador; lo mantiene el SP, no se manda en el ABM. */
    private int  esProgramador;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;

    /**
     * Dónde y en qué trabaja, resuelto por la ACCION 'L' de
     * {@code p_list_trs_Participante}.
     *
     * <p><b>El SP las venía devolviendo desde siempre</b> ({@code su.nombre AS
     * sucursal}, {@code ca.descripcion AS cargo}) y este modelo no las tenía, así
     * que {@code BeanPropertyRowMapper} las descartaba <b>en silencio</b>: sin
     * error, sin log, sin nada. Es el modo de falla clásico de esta capa —
     * agregar una columna al SELECT no alcanza, hay que declararla acá.
     *
     * <p>{@code empresa} sale de la sucursal ({@code tb_sucursal.codEmpresa}) y no
     * de una columna propia: la empresa de una persona ES la de su sucursal, y
     * guardar una segunda copia es fabricar un lugar donde quede vieja.
     */
    private String sucursal;
    private Long   codEmpresa;
    private String empresa;
    private String cargo;

    /**
     * Cómo se LEE la ventana, resuelto por la ACCION 'L' del SP. Cuatro valores:
     * <pre>
     *   VIGENTE               hace sábados hoy
     *   SIN SABADOS           la ventana ya se cerró (fechaBaja pasó)
     *   VUELVE EL dd/mm/aaaa  fechaAlta todavía es futura
     *   FUERA DE LA EMPRESA   activo=0; tapa a todo lo demás, es otra conversación
     * </pre>
     *
     * <p><b>Se deriva en el SQL y no acá</b> por la misma razón que
     * {@code situacionAsueto}: son cuatro combinaciones de dos fechas y un bit
     * contra la fecha de HOY, y si cada pantalla las arma por su cuenta terminan
     * diciendo cosas distintas — el front las calcularía además contra el reloj
     * del teléfono.
     *
     * <p>Y como todo lo que sale del SELECT, hay que declararlo acá o
     * {@code BeanPropertyRowMapper} lo descarta EN SILENCIO.
     */
    private String situacion;

    /**
     * La fecha que explica el rótulo de {@link #situacion}: {@code fechaBaja} si ya
     * salió, {@code fechaAlta} si todavía no entró, <b>NULL</b> si está VIGENTE o
     * fuera de la empresa. Evita que la pantalla tenga que recomponer el texto.
     */
    private Date fechaSituacion;

    // ── solo lectura, ACCION 'T' (turnos por persona: el SUM del Excel) ───
    private int turnosTrabaja;
    private int diasVacacion;
    private int diasCambio;
    private int diasBaja;
    private int diasAsueto;
    private int diasExcusado;

    // ── solo lectura, ACCION 'K' (cumpleaños que caen sábado) ─────────────
    private long   idSabado;
    private Date   fechaSabado;
    private String estadoActual;      // la letra de la celda ese día
    private String alcanceEvento;
    private String motivoEspecial;
    /** ASUETO · ASUETO ANULADO POR EVENTO · LO EXCUSARON · DECIDIO VENIR · FERIADO */
    private String situacionAsueto;
}
