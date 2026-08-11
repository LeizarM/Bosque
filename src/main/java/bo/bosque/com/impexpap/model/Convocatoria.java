package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * La lista nominal de un evento — tabla {@code trs_Convocatoria}.
 *
 * <p>SPs: {@code p_abm_trs_Convocatoria} (I/U/D) · {@code p_list_trs_Convocatoria} (L, D).
 *
 * <p>Es lo que un booleano no podía representar: "vienen todos" casi nunca se cumple
 * literal. En un inventario vienen los de almacenes que no estaban de turno, y se
 * libera a los de turno que no hacen falta.
 *
 * <ul>
 *   <li>{@code CONVOCADO} → viene AUNQUE no le toque por rotación (celda normal '1')</li>
 *   <li>{@code EXCUSADO}  → NO viene aunque le tocara (celda 'E', visible en la grilla)</li>
 * </ul>
 *
 * <p>Sólo tiene sentido en sábados con {@code alcanceEvento IS NOT NULL}: las ausencias
 * de un sábado normal van por {@code trs_Programacion}.
 *
 * <p>El ABM cubre UNA persona. Para convocar en bloque —todo el subárbol de un jefe,
 * o todos los de un cargo— está {@code trs_sp_convocar}. Los dos rehacen las celdas
 * del sábado; escribir la fila sin regenerar dejaría la lista desalineada de la grilla.
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
public class Convocatoria implements Serializable {

    // ── parámetros del ABM ────────────────────────────────────────────────
    private long   idConvocatoria;
    private long   idRol;
    private long   idSabado;
    private long   idParticipante;
    private String tipo;                 // CONVOCADO | EXCUSADO
    private String motivo;               // varchar(200) — se copia a la celda y gana
                                         // sobre el motivoEspecial del día
    private Long   codEmpleadoAutoriza;  // quién tomó la decisión
    private Long   audUsuario;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;

    // ── solo lectura, ACCION 'D' (detalle del evento) ─────────────────────
    private Date   fecha;                // trs_Sabado.fecha
    private String alcanceEvento;
    private String motivoEspecial;
    private long   codEmpleado;
    private String nombreRol;
    private String grupoRotacion;
    /**
     * La columna que importa: 0 en los convocados y 1 en los excusados es lo que
     * hace que la lista sirva. Un CONVOCADO con 1 ya venía solo y esa fila no
     * agrega a nadie — los SPs lo avisan.
     */
    private int    leTocabaPorRotacion;
    private String celdaFinal;           // la letra con la que quedó la celda
    private String situacion;            // texto explicativo del SP
}
