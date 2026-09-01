package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioEmplBosqEmpl;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

/**
 * Cruce usuario-biométrico ⇄ empleado Bosque — {@code tbio_bioEmplBosqEmpl}.
 *
 * <p>SPs: {@code p_abm_BioEmplBosqEmpl} · {@code p_list_BioEmplBosqEmpl}.
 */
public interface IBioEmplBosqEmpl {

    /**
     * Registra, modifica (enlaza/desenlaza) o elimina el cruce. El SP también
     * soporta {@code ACCION='A'} (dispara {@code p_SAP_Rpt_Biometrico} para
     * importar usuarios nuevos desde el biométrico).
     *
     * <p>{@code idGenerado} devuelve {@code idEmpleadBio} en el alta (no hay
     * IDENTITY: ese id lo asigna el dispositivo, no SQL Server).
     *
     * @param acc        'I' | 'U' | 'D' | 'A'
     * @param audUsuario usuario de auditoría (no es un campo del modelo)
     */
    RespuestaSp registrar(BioEmplBosqEmpl item, String acc, Long audUsuario);

    /** [L] Cruces filtrados (por nombre, por enlazados/no enlazados, etc.). */
    List<BioEmplBosqEmpl> listar(Map<String, Object> filtro);
}
