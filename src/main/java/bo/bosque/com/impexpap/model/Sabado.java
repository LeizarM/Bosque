package bo.bosque.com.impexpap.model;

import lombok.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Los sábados del período — tabla {@code trs_Sabado}.
 *
 * <p>SPs: {@code p_abm_trs_Sabado} (U) · {@code p_list_trs_Sabado} (L, C, F, N).
 *
 * <p>Las filas las crea {@code trs_sp_generarRol} caminando de 7 en 7 días; el ABM
 * sólo admite {@code U} (activar/desactivar y declarar el evento). Un INSERT suelto
 * rompería la secuencia de {@code indiceAnio}, de la que depende la rotación A/B.
 *
 * <p><b>{@code indiceAnio} es el número de sábado DEL AÑO</b> (1..53), no del período:
 * impar → trabaja el grupo A, par → el grupo B.
 *
 * <p><b>{@code esFeriado} es un RESUMEN, no la autoridad:</b> vale 1 si ese sábado es
 * no laborable para AL MENOS UN participante. Quién no viene se decide persona por
 * persona, cruzando {@code trh_diaNoLaborable_sucursal} contra
 * {@code trs_Participante.codSucursal} — un feriado departamental deja en 'X' sólo a
 * los de esa sucursal.
 *
 * <p><b>{@code @ACCION='N'}</b> de {@code p_list_trs_Sabado} lista {@code trh_diaNoLaborable}
 * (otra tabla, de RR.HH.), así que devuelve una forma distinta y necesita su propia
 * clase de proyección en {@code dto/}.
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
public class Sabado implements Serializable {

    // ── parámetros del ABM ────────────────────────────────────────────────
    private long   idSabado;
    private long   idRol;
    private String alcanceEvento;    // NULL = normal | TOTAL | SELECTIVA
    private String motivoEspecial;   // varchar(150) — por qué hay evento

    /**
     * BD: {@code bit NOT NULL DEFAULT 1}. Inicializado en 1 a propósito: con el 0
     * de Java, un {@code U} que no lo mande explícitamente desactivaría el sábado
     * y le borraría las celdas generadas.
     */
    private int activo = 1;

    private Long audUsuario;

    // ── resto de la tabla: lo calcula el SP al generar el rol ─────────────
    private Date fecha;
    private int  anio;
    private int  mes;
    private int  trimestre;          // derivado 1..4
    private int  semanaDelMes;       // derivado 1..5
    private int  indiceAnio;         // 1..53 — impar=A, par=B
    private int  esFeriado;          // bit, resumen (ver javadoc)
    private Long codDiaNoLaborable;  // FK a trh_diaNoLaborable; ON DELETE SET NULL
    private Date audFecha;

    // ── solo lectura, ACCION 'L' ──────────────────────────────────────────
    private String grupoQueRota;     // 'A' | 'B', derivado de la paridad

    // ── solo lectura, ACCION 'C' (cobertura: el SUBTOTAL del Excel) ───────
    private int personasTrabajan;
    private int enVacacion;
    private int enCambio;
    private int enBaja;
    private int enAsueto;
    private int excusados;
    private int convocados;

    // ── solo lectura, ACCION 'F' (feriados desincronizados) ───────────────
    private String rol;                              // trs_Rol.nombre
    private String estadoRol;
    private Long   codSucursal;                      // la del rol
    private String grupoQueRotaba;
    private int    marcadoEnElRol;                   // el esFeriado actual
    private int    aplicaEnRRHH;                     // lo que dice trh_diaNoLaborable HOY
    private String motivo;                           // trh_diaNoLaborable.motivo
    private String quePasa;                          // FALTA APLICAR / FALTA QUITAR / PUNTERO VIEJO
    private int    personasQueHoyFiguranTrabajando;
}
