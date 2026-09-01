package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioHrSemanal;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

/**
 * Horario semanal (cabecera) — {@code tbio_bioHrSemanal}.
 *
 * <p>SPs: {@code p_abm_BioHrSemanal} · {@code p_list_BioHrSemanal}.
 */
public interface IBioHrSemanal {

    /**
     * Registra, modifica o elimina el horario semanal.
     *
     * @param acc        'I' | 'U' | 'D'
     * @param audUsuario usuario de auditoría (no es un campo del modelo)
     * @param motivo     por qué se hizo el cambio — queda en {@code tbio_bioBitacora},
     *                   no es un campo de la tabla (ver {@code sql/03_bitacora_biometrico.sql})
     */
    RespuestaSp registrar(BioHrSemanal item, String acc, Long audUsuario, String motivo);

    /** [L] Horarios semanales filtrados. */
    List<BioHrSemanal> listar(Map<String, Object> filtro);
}
