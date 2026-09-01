package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioHrXEmplExpandido;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Calendario expandido por empleado/día — {@code tbio_bioHrXEmplExpandido}.
 *
 * <p>SPs: {@code p_abm_BioHrXEmplExpandido} · {@code p_list_BioHrXEmplExpandido}.
 */
public interface IBioHrXEmplExpandido {

    /**
     * Registra, modifica o elimina UNA fila del calendario expandido.
     *
     * @param acc        'I' | 'U' | 'D'
     * @param audUsuario usuario de auditoría (no es un campo del modelo)
     */
    RespuestaSp registrar(BioHrXEmplExpandido item, String acc, Long audUsuario);

    /** [L] Calendario expandido filtrado (por empleado, por rango de {@code jornada}, etc.). */
    List<BioHrXEmplExpandido> listar(Map<String, Object> filtro);

    /**
     * {@code ACCION='D'}: borra el mes ya generado para UNA fila puntual de
     * {@code tbio_bioHrEmpleado}. Necesario ANTES de {@link #generarMes} —
     * 'A' sólo inserta, así que si un día ya existe (con el idHrEmpleado que
     * sea) lo salta en vez de corregirlo. Llamar una vez por cada fila que el
     * empleado tenga en {@code tbio_bioHrEmpleado}, no una vez por empleado
     * (la SP no filtra por empleado en 'D', sólo por {@code idHrEmpleado}
     * puntual + año/mes).
     *
     * @param idHrEmpleado la fila específica cuyo mes generado se borra
     * @param unDiaDelMes  cualquier fecha del mes a borrar — la SP sólo mira año/mes
     */
    RespuestaSp borrarMes(long idHrEmpleado, Date unDiaDelMes, Long audUsuario);

    /**
     * {@code ACCION='A'}: genera el calendario expandido del mes de
     * {@code unDiaDelMes} (la SP reusa {@code @audFecha} como "qué mes", no
     * como fecha de auditoría) para UN empleado — filtro {@code @idEmpleado}
     * agregado en {@code sql/05_fix_p_abm_BioHrXEmplExpandido_ACCION_A.sql}
     * junto con el fix del bug día-por-día (problema #2 del plan original:
     * antes elegía, por empleado, el {@code idHrEmpleado} de {@code inicio}
     * más reciente de TODOS, sin correlacionar con el día que se estaba
     * generando).
     *
     * <p>Llamar {@link #borrarMes} primero por cada fila de
     * {@code tbio_bioHrEmpleado} del empleado — ver
     * {@code BiometricoController.regenerarCalendarioExpandido}.
     */
    RespuestaSp generarMes(long idEmpleado, Date unDiaDelMes, Long audUsuario);
}
