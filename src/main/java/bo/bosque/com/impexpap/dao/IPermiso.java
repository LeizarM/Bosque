package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CalculoAntiguedadDto;
import bo.bosque.com.impexpap.dto.DesgloseSaldoDto;
import bo.bosque.com.impexpap.dto.EmpleadoColectivoDto;
import bo.bosque.com.impexpap.dto.FichaSaldoDto;
import bo.bosque.com.impexpap.dto.DiaNoHabilDto;
import bo.bosque.com.impexpap.dto.PermisoKardexDto;
import bo.bosque.com.impexpap.dto.TipoPermisoDto;
import bo.bosque.com.impexpap.model.Permiso;

import java.util.Date;
import java.util.List;

/**
 * Lecturas de {@code p_list_Permiso} y la <b>vacación colectiva</b>, que escribe
 * {@code trh_permiso} vía {@code p_abm_Permiso}.
 *
 * <p><b>Un método por ACCION.</b> En ese SP los parámetros CAMBIAN DE SIGNIFICADO según la
 * ACCION —{@code @codPermiso} es el código de la empresa en algunas, el código del permiso en
 * otras—, así que un método genérico que reciba el modelo entero es una trampa. Cada método de
 * acá manda su {@code Map} explícito con los parámetros que esa ACCION realmente usa.
 */
public interface IPermiso {
    /**
     * OBTENDRA LOS DIAS TOTALES DISPONIBLES DE VACACION
     */
    List<Permiso> diasDisponibles(Permiso filtro);

    /**
     * ACCION 'C' — ficha resumen del empleado para la consola de RR.HH.
     *
     * @return lista de 0 o 1 elementos; vacía sólo cuando no hay nada que explicar (→ 204)
     */
    List<FichaSaldoDto> fichaSaldoEmpleado(long codEmpleado);

    /**
     * ACCION 'D' — el saldo desglosado en los 5 tramos, ya emparejado y con los totales.
     *
     * @return {@code null} cuando no hay nada que explicar (→ 204)
     */
    DesgloseSaldoDto desgloseSaldo(long codEmpleado);

    /**
     * ACCION 'U' — calculadora de antigüedad. Simulación, no el saldo de nadie.
     */
    CalculoAntiguedadDto calcularAntiguedad(Date desde, Date hasta);

    /**
     * ACCION 'G' — de quién es la boleta {@code codPermiso}.
     *
     * <p>Existe para autorizar la descarga del reporte de una boleta: sin esto, el endpoint deja
     * enumerar de 1 a 8459 y leer el permiso de cualquiera.
     *
     * @return el {@code codEmpleado} dueño, o {@code null} si la boleta no existe
     */
    Long codEmpleadoDeBoleta(long codPermiso);

    // ══════════════════════════════════════════════════════════════════════
    // VACACIÓN COLECTIVA
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ACCION 'E' — el padrón: todos los empleados con relación activa, su cargo de planilla más
     * reciente y su sucursal, ordenados por empresa y nombre.
     *
     * <p>Es el mismo listado que alimenta los dos asistentes colectivos del legacy (abono y
     * vacación). Devuelve {@link EmpleadoColectivoDto} con {@code dias = 0} y {@code entra = true}:
     * todavía no hay rango que calcular ni motivo por el que excluir a nadie.
     *
     * <p><b>DEFECTO DEL SP, documentado y traducido acá:</b> esa ACCION filtra la empresa con el
     * parámetro {@code @codPermiso} ({@code WHERE @codPermiso IS NULL OR @codPermiso =
     * te.codEmpresa}), no con un {@code @codEmpresa}. El nombre del parámetro miente; el DAO manda
     * {@code codEmpresa} en {@code @codPermiso} y este método se llama por lo que significa. No se
     * toca el SP.
     *
     * @param codEmpresa {@code null} = todas
     */
    List<EmpleadoColectivoDto> padronColectivo(Long codEmpresa);

    /**
     * <b>Qué pasaría.</b> Una fila por persona con los días que le tocarían a ELLA y, para quien
     * queda afuera, el motivo. <b>No escribe absolutamente nada.</b>
     *
     * <p>Los días salen de {@code dbo.f_CalcularDiasHabilesPermiso(codEmpleado, desde, hasta)}, que
     * es la misma función que usa el puente de sábado ya migrado. Se llama por empleado <b>a
     * propósito</b>: descuenta los feriados de {@code trh_diaNoLaborable} que apliquen a la
     * sucursal de esa persona, así que dos personas del mismo lote y el mismo rango pueden terminar
     * con cantidades distintas. El número del cabezal es un estimado y no es el que se graba.
     *
     * <p><b>Por qué la simulación no es opcional.</b> Es el segundo paso del asistente del legacy y
     * la única barrera que existe antes de escribir N permisos de vacación GOZADA, que son días que
     * se le descuentan del saldo a cada persona.
     */
    List<EmpleadoColectivoDto> simularVacacionColectiva(List<Long> codEmpleados, Date desde,
                                                        Date hasta, String motivo);

    /**
     * Lo aplica, <b>todo o nada</b>.
     *
     * <p>Escribe en {@code trh_permiso} vía {@code p_abm_Permiso} con {@code tipoPermiso='vac'}:
     * es vacación <b>GOZADA</b>, o sea un HABER del saldo. <b>No toca
     * {@code trh_vacacionAsignada}</b>, que es el debe — confundirlas le sumaría días a la gente en
     * vez de descontárselos.
     *
     * <p>Por iteración: {@code codPermiso = 0} (alta), y {@code codEmpleado},
     * {@code codRelEmplEmpr} y los días <b>de CADA empleado</b>, nunca los del cabezal.
     * {@code codUsuarioAutorizador} es el usuario que registra, el mismo para todos los permisos
     * generados.
     *
     * <p>Re-simula adentro de la transacción en vez de confiar en la lista que trajo el cliente:
     * entre la pantalla de confirmación y el botón puede haber escrito otra persona.
     *
     * @return cuántos permisos se insertaron
     */
    int aplicarVacacionColectiva(List<Long> codEmpleados, Date desde, Date hasta, String motivo,
                                 long codAutorizador);

    // ══════════════════════════════════════════════════════════════════════
    // Nómina de permisos y las tres altas individuales
    // ══════════════════════════════════════════════════════════════════════

    /**
     * ACCION 'Q' — la <b>Nómina de permisos</b> del empleado (el kardex).
     *
     * <p>Todos los filtros son opcionales y se combinan con AND. {@code tipoPermiso} en
     * {@code null}, vacío o {@code "0"} es "Todos". {@code fechaInicio} y {@code fechaFin} acotan
     * el inicio y el fin del permiso; {@code fecRango} es distinto: es "quién estaba de permiso el
     * día X".
     *
     * <p><b>Sin columnas de deuda</b>: la fórmula del SP depende de {@code trh_repper}, que tiene
     * 0 filas, y devolvería días negativos. Ver {@code PermisoKardexDto}.
     */
    List<PermisoKardexDto> kardex(long codEmpleado, Long codRelEmplEmpr, String tipoPermiso,
                                  Date fechaInicio, Date fechaFin, Date fecRango);

    /**
     * Los permisos que suman un tramo del desglose: {@code 'H'}, {@code 'J'} o {@code 'K'} según
     * la clave. Misma forma de salida que el kardex.
     *
     * @param clave una de {@code SALDO_PENULTIMO}, {@code UTILIZADA} o {@code PROGRAMADA};
     *              cualquier otra es 400, porque los otros dos tramos no son listas de permisos.
     */
    List<PermisoKardexDto> detalleDeTramo(long codEmpleado, String clave);

    /**
     * Los días del rango que NO descuentan vacación, con su motivo: feriados de la sucursal del
     * empleado y sábados que el rol dice que no le tocan. Los domingos no vienen (el cliente los
     * deduce de la fecha). Misma regla que {@code f_CalcularDiasHabilesPermiso}.
     */
    List<DiaNoHabilDto> diasNoHabiles(long codEmpleado, Date desde, Date hasta);

    /**
     * Los permisos de TODA la empresa en una fecha o rango: «quién está fuera». Misma ACCION
     * {@code 'Q'} del kardex pero sin filtrar por persona. <b>Exige al menos un filtro de
     * fecha</b>; sin eso serían las 8.459 filas de la tabla.
     */
    List<PermisoKardexDto> quienEstaFuera(Date fecRango, Date desde, Date hasta);

    /**
     * ACCION {@code 'W'} — las boletas emitidas entre dos fechas, de toda la empresa. Acota por
     * permisos <b>contenidos</b> en la ventana, no por los que la cruzan. <b>Exige al menos una
     * fecha.</b>
     */
    List<PermisoKardexDto> boletasEntreFechas(Date desde, Date hasta, String tipoPermiso);

    /**
     * El combo de tipo de permiso ({@code v_tipos} grupo 13).
     *
     * @param incluirVacacionYPago {@code false} para el modal de permiso (los 7 tipos, sin
     *        {@code vac} ni {@code pva}); {@code true} para el filtro de la nómina (los 9).
     */
    List<TipoPermisoDto> tiposPermiso(boolean incluirVacacionYPago);

    /**
     * Lo que muestran en vivo los campos calculados del modal —"Horas de permiso", "Días de
     * permiso", "Total días de vacación", "Horas a reponer"— y si el alta va a poder guardarse
     * ({@code entra} / {@code detalle}).
     *
     * <p>Es el mismo cálculo que graba el alta: {@code dbo.f_CalcularDiasHabilesPermiso}. El radio
     * "Horario Estándar / Continuo" no viaja porque la función lo deduce de la hora de fin.
     *
     * @param tipoPermiso sólo decide "Horas a reponer" ({@code otro} y {@code pcr}); no cambia el
     *        cálculo de días.
     */
    EmpleadoColectivoDto previsualizarPermiso(long codEmpleado, String tipoPermiso, Date desde,
                                              Date hasta);

    /**
     * Alta individual en {@code trh_permiso}: "Programar permiso" (tipo del combo) y "Programar
     * vacación" ({@code tipoPermiso = 'vac'}). Mismo camino de escritura que la colectiva.
     *
     * @return la fila releída — {@code p_abm_Permiso} no devuelve el id generado.
     */
    PermisoKardexDto registrarPermiso(long codEmpleado, String tipoPermiso, Date desde, Date hasta,
                                      String motivo, long codAutorizador);

    /**
     * Alta de <b>vacación PAGADA</b> ({@code tipoPermiso = 'pva'}): son días que se pagan, no que
     * se toman. Fuerza {@code hasta = fecha} y los días los indica el usuario.
     *
     * @param confirmado el usuario ya vio el impacto (saldo antes y después, o el pago repetido)
     *        y lo aceptó. El doble toque se rechaza con 409 aunque esto venga en {@code true}.
     */
    PermisoKardexDto registrarVacacionPagada(long codEmpleado, Date fecha, double dias,
                                             String motivo, long codAutorizador, boolean confirmado);
}
