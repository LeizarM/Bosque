package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * El {@code @RequestBody} de los tres endpoints de {@code /permiso-rrhh}.
 *
 * <p>Un solo DTO de entrada para las tres ACCIONes porque entre las tres piden tres campos y
 * ninguno se contradice: la ficha y el desglose usan {@code codEmpleado}, la calculadora usa
 * {@code desde}/{@code hasta}. Tres clases de un campo cada una serían tres archivos para
 * mantener sincronizados con el mismo controlador.
 *
 * <p><b>SUPUESTO D4 — pendiente de confirmación de RR.HH. (ver plan §5).</b> Acá NO viaja
 * quién está preguntando. La identidad del actor sale de {@code Authentication} (el token),
 * nunca del body: si el cliente pudiera afirmar quién es, el ACL de botones no valdría nada.
 * Por eso este DTO no tiene {@code codUsuario} ni {@code audUsuario} y no hay que agregárselos.
 *
 * <p><b>Todo POST con JSON, nunca query params.</b> {@code SecurityFilter} revisa
 * {@code getParameterMap()} y el query string buscando patrones tipo SQLi, y devuelve 403 con
 * su propio cuerpo ante un apóstrofo o una palabra como {@code select}. Un
 * {@code GET ?nombre=D'Angelo} daría un 403 que nadie sabría explicar.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class PermisoRrhhFiltroDto implements Serializable {

    /**
     * Único filtro real de las ACCIONes {@code 'C'} y {@code 'D'}.
     *
     * <p>Primitivo a propósito: si el JSON no lo trae llega 0, y 0 no es un empleado válido,
     * así que la validación del controlador ({@code codEmpleado <= 0 -> 400 "Debe seleccionar
     * un empleado."}) cubre tanto el ausente como el inválido con una sola regla.
     */
    private long codEmpleado;

    // ── sólo para ACCION 'U' (calculadora de antigüedad) ──────────────────

    /**
     * Fecha de inicio de beneficio SIMULADA. Wrapper y no primitivo: el controlador tiene
     * que distinguir "no la mandó" de cualquier fecha concreta para poder responder
     * {@code 400 "Debe indicar la fecha de inicio de beneficio y la fecha a calcular."}
     *
     * <p>Formato aceptado: {@code "2015-03-01"} o {@code "2015-03-01 00:00:00"} — lo resuelve
     * {@code JacksonConfig.FlexibleDateDeserializer}.
     */
    private Date desde;

    /** Fecha hasta la cual calcular. Misma regla que {@link #desde}. */
    private Date hasta;

    // ── sólo para ACCION 'Q' (nómina de permisos) ─────────────────────────

    /**
     * La relación laboral por la que filtrar el kardex. {@code null} = todas. Wrapper y no
     * primitivo por la misma razón que el SP: un {@code 0} enviado como filtro no devuelve "todas"
     * sino <b>ninguna</b>, porque la ACCION 'Q' compara {@code @codRelEmplEmpr = tp.codRelEmplEmpr}.
     */
    private Long codRelEmplEmpr;

    /**
     * El combo de tipo de permiso. {@code null}, vacío o {@code "0"} (lo que manda la opción
     * "Todos") = todos los tipos.
     *
     * <p>Declarado acá porque Jackson descarta en silencio lo que el DTO no declara: sin este campo
     * la grilla saldría sin filtrar y nadie vería un error.
     */
    private String tipoPermiso;

    /**
     * "Fecha Rango" del legacy, y <b>no es un rango</b>: es un día suelto que el SP compara con
     * {@code @fecRango BETWEEN tp.desde AND tp.hasta}, o sea "quién estaba de permiso el día X".
     * Se combina con AND con {@link #desde} y {@link #hasta}, que sí acotan el inicio y el fin.
     */
    private Date fecRango;

    /**
     * Qué tramo del desglose se está abriendo: {@code SALDO_PENULTIMO}, {@code UTILIZADA} o
     * {@code PROGRAMADA}. Lo usa {@code /saldo/detalle-tramo} para elegir la ACCION del SP.
     *
     * <p>El cliente manda la clave y <b>no</b> las fechas: {@code fecIniPenUl} lo resuelve el
     * servidor con la misma consulta que produjo el monto, así el detalle no puede discrepar del
     * total. Declarado acá porque Jackson descarta en silencio lo que el DTO no declara.
     */
    private String claveTramo;

    /**
     * El combo de tipos: {@code false} —el default— devuelve los 7 del modal de permiso (sin
     * {@code vac} ni {@code pva}, como {@code Tipos.cargarList13A()}); {@code true}, los 9 del filtro
     * de la nómina.
     *
     * <p><b>El default es el excluyente a propósito.</b> Si el cliente se olvida del flag, el peor
     * caso es un filtro al que le faltan dos opciones —se ve—, y no un modal que ofrece "Vacación" a
     * quien tiene {@code btnProgramarPermiso} pero no {@code btnProgramarVacacion}.
     */
    private boolean incluirVacacionYPago;
}
