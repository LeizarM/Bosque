package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Programacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * El evento de programar: un jefe decide otro horario para un dependiente.
 * SPs: {@code p_abm_trs_Programacion} · {@code p_list_trs_Programacion}.
 */
public interface IProgramacion {

    /**
     * Registra, modifica o ANULA una programación.
     *
     * <p>'I' delega en {@code trs_sp_programar}: valida que quien programa sea
     * programador activo (o el reemplazo de uno), que el dependiente cuelgue de él
     * en el organigrama, crea la programación Y escribe la celda — todo en una
     * transacción. Hay que mandar {@code codEmpleadoDependiente} y {@code codigoExcel}.
     *
     * <p>'D' no borra: marca {@code estado='ANULADA'} y libera las celdas, porque
     * las celdas 'P' apuntan aquí por FK.
     *
     * @param programacion Datos de la programación
     * @param acc          'I' | 'U' | 'D'
     */
    RespuestaSp registrarProgramacion(Programacion programacion, String acc);

    /**
     * [L] Qué decidió cada jefe y con cuánta antelación.
     * {@code controlAntelacion} marca los avisos con menos de una semana.
     * @param idRol    0 = todos
     * @param idSabado 0 = todos
     */
    List<Programacion> obtenerProgramaciones(long idRol, long idSabado);

    /** [L] Una programación por su id. */
    Programacion obtenerProgramacionPorId(long idProgramacion);
}
