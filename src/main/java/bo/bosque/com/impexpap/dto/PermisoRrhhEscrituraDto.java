package bo.bosque.com.impexpap.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * El {@code @RequestBody} de las <b>escrituras</b> de {@code /permiso-rrhh}.
 *
 * <p>Uno solo para las ocho rutas, por lo mismo que {@link PermisoRrhhFiltroDto} es uno solo para
 * las tres lecturas: entre todas piden nueve campos, ninguno se contradice y el que no aplica
 * simplemente no viaja. Ocho clases de tres campos serían ocho archivos para mantener sincronizados
 * con el mismo controlador.
 *
 * <h3>Acá NO viaja quién escribe</h3>
 * Ni {@code codUsuario} ni {@code audUsuario}, y no es un olvido: el firmante de una escritura no
 * lo puede afirmar el cliente. Sale de {@code Authentication} y sólo de ahí. Si alguien agrega el
 * campo, el controlador lo va a seguir ignorando.
 *
 * <h3>Alta y edición son el mismo endpoint, y lo decide el id</h3>
 * {@code codVacacionAsignada}/{@code codAbonoDias} en {@code 0} es un alta; mayor a cero, una
 * edición. Es literalmente lo que hace el legacy
 * ({@code String acc = "U"; if (getCodX() == 0) acc = "I";}) y el precedente vivo del repo
 * ({@code PagosExtranjerosController.registrarAsiento}).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PermisoRrhhEscrituraDto implements Serializable {

    // ── qué fila ──────────────────────────────────────────────────────────

    /** {@code 0} = alta sobre un aniversario; {@code > 0} = edición de esa fila. */
    private long codVacacionAsignada;

    /** {@code 0} = alta; {@code > 0} = edición de ese abono. */
    private long codAbonoDias;

    /**
     * De quién es. Obligatorio también en las bajas: el {@code DELETE} de los dos SP borra por id
     * <b>sin mirar de quién es la fila</b>, así que la pertenencia se comprueba con esto.
     */
    private long codEmpleado;

    /**
     * Relación laboral. Wrapper porque la columna es nullable en las dos tablas.
     *
     * <p>Se usa en el historial de vacación asignada (todas las lecturas del saldo filtran por la
     * relación vigente) y en el alta de vacación asignada. <b>En el abono no se acepta</b>: ahí la
     * resuelve el DAO contra la base, porque mandar la equivocada le movería el saldo a otra
     * persona-relación.
     */
    private Long codRelEmplEmpr;

    // ── qué se registra ───────────────────────────────────────────────────

    /**
     * Días asignados o abonados, según la ruta. Primitivo a propósito: si el JSON no lo trae llega
     * {@code 0}, y {@code 0} <b>es un alta válida de vacación asignada</b> ("este año no le
     * corresponde nada", 446 de las 1.424 filas reales), mientras que en abono el DAO lo rechaza
     * por su propio umbral. Un wrapper obligaría a decidir acá lo que ya decide cada regla.
     *
     * <h3>Los dos alias son una RED, no el contrato</h3>
     * La clave canónica es {@code dias} y es la que manda el cliente. Los alias existen porque el
     * modo en que esto falla es <b>silencioso y caro</b>: no hay
     * {@code FAIL_ON_UNKNOWN_PROPERTIES}, así que un cuerpo con {@code diasAsignados} se aceptaba
     * con un 200, {@code dias} quedaba en {@code 0} por ser primitivo y la vacación asignada se
     * grababa con CERO días — que además es un valor legítimo, así que ninguna validación lo
     * frenaba. Con el alias, un cliente viejo o un script guardado escribe el número correcto en
     * vez de escribir cero.
     */
    @JsonAlias({ "diasAsignados", "diasAbonados" })
    private double dias;

    /** Obligatorio en los dos. El largo lo valida cada DAO con el de SU columna (300 y 50). */
    private String motivo;

    /**
     * Fecha del movimiento.
     *
     * <p>En el alta de vacación asignada <b>no es un campo libre</b>: tiene que ser uno de los
     * aniversarios que devuelve el historial, y el DAO lo comprueba contra el servidor. En la
     * edición se ignora (el legacy deshabilita el campo y el SP conserva la fila).
     *
     * <p>En el historial de vacación asignada es la fecha de corte; {@code null} = hoy.
     */
    private Date fecha;

    /**
     * El usuario ya vio el aviso y lo confirmó igual.
     *
     * <p>Habilita lo <b>inusual</b> —un monto fuera del rango histórico, un aniversario que ya
     * tiene registro—, nunca el doble toque: una fila idéntica cargada hace segundos se rechaza con
     * 409 aunque esto venga en {@code true}, porque el segundo toque del teléfono manda exactamente
     * el mismo cuerpo, confirmación incluida.
     *
     * <p>El alias es la misma red que la de {@link #dias}: un {@code confirmarDuplicado} que se
     * descarta deja al usuario apretando "Confirmar" contra un 400 que no se va nunca.
     *
     * <p><b>Lo leen sólo las rutas que pueden devolver un 400 confirmable</b>:
     * {@code /vacacion-asignada/registrar}, {@code /abono-dias/registrar} y {@code /vacacion/pagar}.
     * {@code /permisos/registrar} y {@code /vacacion/registrar} <b>no</b> — {@code
     * IPermiso.registrarPermiso} ni siquiera recibe el flag—, y por eso el cliente tampoco lo manda
     * ahí: una clave declarada que se descarta en silencio es un botón "Guardar igual" que no
     * guarda nunca. Si algún día el DAO agrega un aviso confirmable a esas dos altas, el parámetro
     * hay que pasarlo hasta el DAO en el mismo cambio.
     */
    @JsonAlias("confirmarDuplicado")
    private boolean confirmado;

    // ── carga colectiva ───────────────────────────────────────────────────

    /**
     * A quiénes. Los demás campos son el cabezal: mismos días, misma fecha y mismo motivo para
     * todos; lo único que cambia por persona es su relación laboral, que se resuelve en el
     * servidor.
     *
     * <p><b>En la vacación colectiva ni siquiera los días son iguales para todos:</b> el cabezal es
     * un estimado y lo que se graba se calcula por persona (ver
     * {@link bo.bosque.com.impexpap.dao.IPermiso#simularVacacionColectiva}).
     */
    private List<Long> codEmpleados;

    // ── vacación colectiva ────────────────────────────────────────────────

    /**
     * Filtro del padrón. {@code null} = todas las empresas.
     *
     * <p><b>Cuidado con el SP:</b> {@code p_list_Permiso @ACCION='E'} filtra la empresa con el
     * parámetro {@code @codPermiso} ({@code WHERE @codPermiso IS NULL OR @codPermiso =
     * te.codEmpresa}). Es un defecto del SP, no un renombre acá: el DAO traduce, y este campo se
     * llama por lo que significa.
     */
    private Long codEmpresa;

    /**
     * Inicio del rango de la vacación colectiva, <b>con hora</b> ({@code "2026-04-04 08:30:00"}).
     *
     * <p>La hora no es decorativa: {@code f_CalcularDiasHabilesPermiso} cuenta minutos hábiles
     * entre {@code desde} y {@code hasta}, así que perderla convierte medio día en cero.
     */
    private Date desde;

    /** Fin del rango, con hora. Ver {@link #desde}. */
    private Date hasta;

    // ── altas individuales de permiso ─────────────────────────────────────

    /**
     * El tipo del combo de "Programar permiso" ({@code v_tipos} grupo 13). {@code "vac"} en
     * "Programar vacación" —la única diferencia entre los dos modales— y {@code null} en todo lo
     * demás.
     *
     * <p><b>Tiene que estar declarado acá:</b> Jackson descarta en silencio lo que el DTO no
     * declara (no hay {@code FAIL_ON_UNKNOWN_PROPERTIES}), así que un {@code tipoPermiso} sin campo
     * no daría error — grabaría el permiso con el tipo en NULL y la fila desaparecería de la
     * nómina, que hace JOIN con {@code v_tipos}.
     *
     * <p><b>{@code pva} no entra por acá</b>: el pago de vacación va por su propio endpoint,
     * porque necesita el control de saldo y de doble toque que el alta común no hace.
     */
    private String tipoPermiso;

    // La VACACIÓN PAGADA usa `fecha` y `dias`, no `desde`/`hasta`: el legacy fuerza hasta = desde y
    // los días los tipea el usuario. No hace falta ningún campo nuevo — un `diasAPagar` sería un
    // segundo nombre para `dias`.
}
