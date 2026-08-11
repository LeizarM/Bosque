package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Permuta o cobertura de un sábado entre dos compañeros — tabla {@code trs_Cambio}.
 *
 * <p>SPs: {@code p_abm_trs_Cambio} (I/U/D/<b>A</b>) · {@code p_list_trs_Cambio} (L).
 *
 * <p>El estado 'C' del catálogo tiene {@code requiereCambio=1}, o sea que una celda en
 * 'C' SIN su fila acá es una incoherencia.
 *
 * <p><b>{@code ACCION='A'} (aprobar) es lo que de verdad mueve la grilla:</b> son TRES
 * escrituras de celda que van juntas en una transacción — el titular pasa a 'C', el
 * que cubre aparece con '1' un sábado que no le tocaba, y si hay reposición se le
 * libera ese otro sábado. El alta ({@code I}) sólo registra el acuerdo en estado
 * SOLICITADO y NO toca ninguna celda.
 *
 * <p><b>PENDIENTE DE DEFINIR:</b> el DDL no dice qué significa {@code idSabadoReposicion}.
 * Hoy se interpreta como "el sábado que se le devuelve AL QUE CUBRIÓ". La otra lectura
 * posible —el sábado que el TITULAR tiene que devolver— daría una celda nueva para el
 * titular en vez de quitarle una al reemplazo. Cambia la cobertura.
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
public class Cambio implements Serializable {

    // ── parámetros del ABM ────────────────────────────────────────────────
    private long   idCambio;
    private long   idRol;
    private long   idSabado;              // el sábado que hay que cubrir
    private long   codEmpleadoTitular;    // quien no viene
    private Long   codEmpleadoReemplazo;  // quien lo cubre
    private Long   idSabadoReposicion;    // el sábado que se devuelve (ver javadoc)
    private String tipo;                  // COBERTURA | PERMUTA
    private String estado;                // SOLICITADO | APROBADO | RECHAZADO | ANULADO
    private String motivo;                // varchar(200)
    private Long   codAprobador;
    private Date   fechaSolicitud;
    private Date   fechaResolucion;
    private Long   audUsuario;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;

    // ── solo lectura, ACCION 'L': joins ───────────────────────────────────
    private Date   sabadoCubierto;    // trs_Sabado.fecha del idSabado
    private String titular;           // nombreRol del titular
    private String reemplazo;         // nombreRol del reemplazo
    private Date   fechaReposicion;   // trs_Sabado.fecha del idSabadoReposicion
}
