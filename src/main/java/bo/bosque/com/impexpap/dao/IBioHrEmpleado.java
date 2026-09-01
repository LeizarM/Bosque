package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioHrEmpleado;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

/**
 * Horario semanal asignado a un empleado — {@code tbio_bioHrEmpleado}.
 *
 * <p>SPs: {@code p_abm_BioHrEmpleado} · {@code p_list_BioHrEmpleado}.
 */
public interface IBioHrEmpleado {

    /**
     * Registra, modifica, elimina, o inactiva por fecha (ACCION='A', resetea
     * {@code inicio} a 2000-01-01 — equivalente a {@code inactivarPorFecha}
     * del legacy) la asignación de horario.
     *
     * @param acc        'I' | 'U' | 'D' | 'A'
     * @param audUsuario usuario de auditoría (no es un campo del modelo)
     * @param motivo     por qué se hizo el cambio — queda en {@code tbio_bioBitacora},
     *                   no es un campo de la tabla (ver {@code sql/03_bitacora_biometrico.sql})
     */
    RespuestaSp registrar(BioHrEmpleado item, String acc, Long audUsuario, String motivo);

    /** [L] Asignaciones filtradas (por empleado, por horario semanal, etc.). */
    List<BioHrEmpleado> listar(Map<String, Object> filtro);
}
