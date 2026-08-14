package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Proyección de {@code p_list_Permiso @ACCION='C'} — la ficha resumen del empleado: quién es,
 * dónde trabaja y cuántos días de vacación tiene.
 *
 * <p><b>Los nombres de los 10 primeros campos son los alias del SELECT del SP, tal cual.</b>
 * {@code SpHelper.ejecutarListado} usa {@code BeanPropertyRowMapper}, que mapea POR NOMBRE de
 * columna. Renombrar un campo acá lo deja en 0/null sin que nada falle — el bug más silencioso
 * posible. El DAO legacy leía estas columnas por índice; este DTO existe justamente para que
 * reordenar el SELECT no rompa nada.
 *
 * <p><b>Por qué un DTO nuevo y no campos en {@code model/Permiso}.</b> {@code Permiso} se
 * serializa a parámetros {@code @campo=?} en {@code /vacacion/diasDisponibles} (ACCION 'H1',
 * overload de modelo de {@code SpHelper}). Sus 15 campos son 15 parámetros declarados por
 * {@code p_list_Permiso}; un campo de más haría que el {@code EXEC} falle con "too many
 * arguments" y tiraría abajo un endpoint de producción. <b>{@code Permiso} no se toca.</b>
 *
 * @see bo.bosque.com.impexpap.utils.Fmt#dias(double) el formato de los campos {@code *Txt}
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FichaSaldoDto implements Serializable {

    // ══════════════════════════════════════════════════════════════════════
    // LAS 10 COLUMNAS DEL SP — nombres calcados del SELECT, no tocar
    // ══════════════════════════════════════════════════════════════════════

    private long   codEmpleado;
    /** {@code apPaterno + ' ' + apMaterno + ' ' + nombres}. Ya viene armado del SP. */
    private String datoEmpleado;
    /** {@code ISNULL(te.nombre, 'Sin Empresa')}. */
    private String datoEmpresa;
    /**
     * Descripción del cargo REAL ({@code codCargoSucursal}).
     *
     * <p>Ojo al compararlo con el buscador: {@code p_list_Empleado @ACCION='Y'} usa el cargo
     * de PLANILLA ({@code codCargoSucPlanilla}), así que el nombre de empresa puede diferir
     * entre la lista y esta ficha. Es comportamiento legacy y se preserva.
     */
    private String datoCargo;
    /**
     * String, NO fecha: el SP devuelve {@code CONVERT(varchar(11), fechaIni, 103)} —o sea
     * {@code "01/03/2015"}— o el literal {@code "Sin Asignar"}. Parsearlo acá para volver a
     * formatearlo sólo agregaría una forma de romperlo.
     */
    private String datoFechaBeneficio;
    /** Prosa de {@code f_calcularAntiguedadString}, o {@code "Sin Beneficio"}. */
    private String datoAntiguedad;
    /**
     * {@code SUM(vacacionAsignada.diasAsignados) − SUM(permiso.cantidadDias VAC/PVA)}.
     *
     * <p><b>SUPUESTO D0 — pendiente de confirmación de RR.HH. (ver plan §5).</b> Las dos sumas
     * están filtradas por {@code codRelEmplEmpr} = la relación laboral ACTIVA. Todo lo que el
     * empleado hizo bajo un contrato anterior NO entra en este número. Medido hoy: el 78 % de
     * los permisos de empleados con relación activa vive en otra relación. Por eso viajan
     * también los tres campos de alcance de más abajo, y la UI los rotula.
     */
    private double diasNoUsados;
    /** {@code SUM(trh_abonoDias.diasAbonados)}, misma restricción por relación activa (D0). */
    private double diasAbonados;
    /**
     * <b>El SP siempre manda {@code 0.0} acá</b> ({@code 0.0 as totalDias}, literal). Lo
     * calcula el consumidor. En esta migración lo calcula el DAO —
     * {@code totalDias = diasNoUsados + diasAbonados} — igual que hacía
     * {@code PermisoManagedBean} en el legacy, para que el número no dependa del cliente.
     */
    private double totalDias;
    /** La llave del módulo: la relación laboral vigente contra la que se calculó todo. */
    private long   datoRelEmplEmprVigente;

    // ══════════════════════════════════════════════════════════════════════
    // CALCULADOS EN JAVA — no son columnas; BeanPropertyRowMapper los ignora
    // ══════════════════════════════════════════════════════════════════════

    /**
     * SUPUESTO D1 — pendiente de confirmación de RR.HH. (ver plan §5).
     *
     * <p>El móvil pinta estos {@code *Txt} y el escritorio ordena por el numérico. Los dos
     * salen del MISMO double, así que no pueden divergir.
     */
    private String diasNoUsadosTxt;
    private String diasAbonadosTxt;
    private String totalDiasTxt;

    // ══════════════════════════════════════════════════════════════════════
    // ALCANCE POR RELACIÓN LABORAL (D0)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * <b>SUPUESTO D0 — pendiente de confirmación de RR.HH. (ver plan §5).</b> Se rotula
     * explícitamente el alcance en vez de decidir por RR.HH. qué historia cuenta.
     *
     * <p>Estos tres campos NO salen de ninguna ACCION de {@code p_list_Permiso} — no hay SP
     * que los dé. El DAO los llena con una consulta de lectura aparte sobre
     * {@code tb_relEmplEmpr}, {@code trh_permiso} y {@code trh_vacacionAsignada}.
     * <b>TODO:</b> pedir al DBA una ACCION nueva que los devuelva y borrar esa consulta.
     *
     * <p>Alimentan el rótulo <i>"Saldo de la relación laboral actual, desde dd/mm/aaaa · N
     * movimientos anteriores no incluidos"</i>. Sin ese rótulo, un saldo correcto se lee como
     * un saldo equivocado, que es exactamente lo que bloquea el criterio de aceptación #6.
     *
     * <p>Se serializa como {@code "2019-04-01 00:00:00"}: {@code JacksonConfig} fija
     * {@code "yyyy-MM-dd HH:mm:ss"} para TODAS las respuestas del backend. El ejemplo del plan
     * §6.3 muestra {@code "2019-04-01"} a secas; manda la convención del repo, y el modelo
     * Dart parsea las dos formas con {@code DateTime.parse}.
     */
    private Date    relacionVigenteDesde;
    /** true = el empleado tuvo contratos anteriores al vigente. */
    private boolean tieneRelacionesAnteriores;
    /** Permisos + asignaciones que quedaron fuera de este saldo por ser de otra relación. */
    private int     movimientosFueraDeRelacionVigente;
}
