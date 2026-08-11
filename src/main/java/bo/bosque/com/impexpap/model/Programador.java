package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Los "empleados especiales": jefes con permiso de programar a otros —
 * tabla {@code trs_Programador}.
 *
 * <p>SPs: {@code p_abm_trs_Programador} (I/U/D) · {@code p_list_trs_Programador} (L, D).
 *
 * <p><b>No guarda la lista de dependientes:</b> se DERIVA del organigrama
 * ({@code trh_cargo.codCargoPadre}) a través de la función en línea
 * {@code fn_trs_ProgramadorDependiente()}. {@code alcance} decide hasta dónde llega:
 * {@code DIRECTOS} sólo los hijos, {@code SUBARBOL} todo su árbol.
 *
 * <p>Al dar de alta, el SP devuelve en {@code errormsg} CUÁNTOS dependientes le da el
 * organigrama. Si son 0, el permiso no sirve — y te enterás ahí, no cuando intente usarlo.
 *
 * <p>La baja es LÓGICA ({@code estado='I'}): un borrado físico dejaría huérfanas las
 * programaciones que ese jefe ya hizo.
 *
 * <p><b>{@code @ACCION='D'}</b> de {@code p_list_trs_Programador} devuelve el organigrama
 * expandido (una fila por dependiente, con profundidad), que es una forma distinta y
 * necesita su propia clase de proyección en {@code dto/}.
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
public class Programador implements Serializable {

    // ── parámetros del ABM ────────────────────────────────────────────────
    private long   idProgramador;
    private long   codEmpleado;           // el jefe que programa
    private Long   codSucursal;           // NULL = todas
    private String alcance;               // DIRECTOS | SUBARBOL
    private Long   codEmpleadoReemplazo;  // quién programa en su lugar
    private String estado;                // char(1): A | I
    private Date   fechaDesde;
    private Date   fechaHasta;
    private String observacion;           // varchar(200)
    private Long   audUsuario;

    // ── solo lectura ──────────────────────────────────────────────────────
    private Date audFecha;

    // ── solo lectura, ACCION 'L': joins y conteo ──────────────────────────
    private String jefe;          // nombre del titular
    private String reemplazo;     // nombre del reemplazo
    /**
     * Nombre de la sucursal del permiso, resuelto por el listado.
     *
     * <p>Viene vacío cuando {@code codSucursal} es NULL, y ahí el vacío ES la
     * respuesta: el permiso vale para todas las sucursales, no hay una que
     * nombrar. La pantalla mostraba «sucursal 20» y nadie sabe de memoria que
     * 20 es CENTRAL.
     */
    private String sucursal;
    /** Cuántos le da el organigrama HOY. Si es 0, el permiso no sirve de nada. */
    private int    dependientes;

    /**
     * <b>Sólo entrada, y sólo para {@code /previsualizar-dependientes}.</b> No es una
     * columna de {@code trs_Programador}: un permiso no pertenece a un rol, vale para
     * todos. Lo usa la previsualización para marcar cuáles de los dependientes además
     * participan del rol que se está mirando.
     *
     * <p>Que viaje en este modelo y no en un DTO aparte no rompe las escrituras:
     * {@code registrarProgramador} pasa por {@code SpHelper.ejecutarAbm}, que usa
     * {@code SimpleJdbcCall} y descarta los parámetros que el SP no declara. Si esa
     * ruta cambiara a {@code ejecutarAbmMap}, un {@code @idRol=?} haría fallar el SP.
     */
    private long   idRol;
}
