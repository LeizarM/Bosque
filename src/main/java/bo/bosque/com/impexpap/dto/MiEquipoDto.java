package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Proyección de {@code p_list_trs_Programador @ACCION='E'} — la respuesta a
 * <b>"¿soy programador y quién es mi gente?"</b>.
 *
 * <p><b>Por qué existe.</b> Hasta ahora, para programar un sábado el cliente tenía que
 * mandar su propio {@code codEmpleadoProgramador}: o sea, decirle al servidor quién era.
 * Eso es autoelevación servida en bandeja — cualquiera cambiaba ese número por el de un
 * jefe y programaba a la gente de otro. Con este DTO la identidad viaja al revés: el
 * cliente manda sólo su token, el servidor resuelve {@code login → tb_usuario →
 * codEmpleado → trs_Programador} y devuelve QUIÉN ES y A QUIÉNES puede tocar. El cliente
 * nunca afirma su identidad, la recibe.
 *
 * <p><b>Siempre viene una fila, nunca vacío.</b> "No sos programador" es una respuesta
 * válida ({@code esProgramador=0} y el equipo en cero), no un 204: la pestaña "Su Equipo"
 * necesita esa respuesta para saber que no tiene que mostrarse.
 *
 * <p><b>{@code esReemplazo}</b> distingue al titular del suplente. Los dos ven y programan
 * el MISMO equipo — el del {@code idProgramador} — porque el organigrama no conoce al
 * reemplazo, sólo al jefe. La diferencia queda en la traza: cuando programa el suplente,
 * {@code trs_sp_programar} lo registra en {@code trs_Programacion.codEmpleadoEjecutor}.
 *
 * <p><b>Ojo con el nombre del campo lista:</b> se llama {@code equipo} y NO
 * {@code dependientes}, a propósito. {@code cantidadDependientes} ya es una columna
 * {@code int} del SELECT, y con dos propiedades que arrancan igual
 * {@code BeanPropertyRowMapper} intentaría meter el {@code int} de la columna en la lista.
 *
 * <p>Los campos que salen como <b>wrapper</b> ({@code Long}) son los que el SP devuelve en
 * NULL cuando el login no resuelve a ningún programador: {@code BeanPropertyRowMapper}
 * revienta si intenta meter un NULL en un primitivo. Las tres columnas calculadas
 * ({@code esProgramador}, {@code esReemplazo}, {@code cantidadDependientes}) son banderas
 * y un COUNT, que nunca vienen en NULL.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MiEquipoDto implements Serializable {

    // ── quién sos (sale de tb_usuario, la tabla puente login → empleado) ──
    private Long codUsuario;
    private Long codEmpleado;

    // ── qué podés hacer ───────────────────────────────────────────────────
    /** 1 = figurás en {@code trs_Programador} activo, como titular o como reemplazo. */
    private int esProgramador;
    /** 1 = entrás por {@code codEmpleadoReemplazo}: programás en lugar del titular. */
    private int esReemplazo;
    /**
     * 1 = figurás en {@code trs_Rrhh} activo, así que podés corregir la celda de
     * CUALQUIERA, seas o no programador.
     *
     * <p>Viaja en esta misma respuesta y no en un endpoint aparte porque la pregunta
     * del front es una sola —«qué puedo hacer yo»— y partirla en dos llamadas la parte
     * también en el tiempo: habría un instante con media respuesta y la pantalla
     * decidiendo con eso.
     *
     * <p>No incluye a ROLE_ADM: ese lo sabe el front por el token y son cosas
     * distintas. En esta empresa los 6 ROLE_ADM son Sistemas, no RR.HH.
     */
    private int esRrhh;

    // ── el permiso concreto que te habilita ───────────────────────────────
    private Long   idProgramador;
    /** El jefe dueño del permiso. Si {@code esReemplazo=1}, NO sos vos. */
    private Long   codEmpleadoTitular;
    private String jefe;
    /** DIRECTOS = sólo tus reportes directos · SUBARBOL = todo tu árbol. */
    private String alcance;
    private Long   codSucursal;
    /** Nombre de la sucursal. Vacío = el permiso vale para todas. */
    private String sucursal;
    /** Cuántos te da el organigrama HOY. Si es 0, el permiso no te sirve de nada. */
    private int    cantidadDependientes;

    // ── tu gente ──────────────────────────────────────────────────────────
    /**
     * Se llena aparte, con {@code @ACCION='D'} por {@code idProgramador}. Arranca en lista
     * vacía y no en NULL para que el front no tenga que preguntar antes de recorrerla.
     */
    private List<ProgramadorDependienteDto> equipo = new ArrayList<>();
}
