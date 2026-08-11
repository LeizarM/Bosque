package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.EstadoTurno;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Catálogo de valores de celda del Rol de Turnos de Sábado.
 * SPs: {@code p_abm_trs_EstadoTurno} · {@code p_list_trs_EstadoTurno}.
 */
public interface IEstadoTurno {

    /**
     * Registra, actualiza o retira un estado del catálogo.
     * En 'D', si el estado está en uso por alguna celda el SP no lo borra:
     * lo marca {@code estado='I'} para no romper la FK.
     * @param estadoTurno Datos del estado
     * @param acc         'I' | 'U' | 'D'
     */
    RespuestaSp registrarEstadoTurno(EstadoTurno estadoTurno, String acc);

    /** [L] Lista el catálogo completo. */
    List<EstadoTurno> obtenerEstadosTurno();

    /** [L] Devuelve un estado por su código del Excel ('1','V','C','B','X','A','L','E'). */
    EstadoTurno obtenerPorCodigo(String codigoExcel);
}
