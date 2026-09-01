package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioHrs;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

/**
 * Plantilla de turno (ingreso/salida) — {@code tbio_bioHrs}.
 *
 * <p>SPs: {@code p_abm_BioHrs} · {@code p_list_BioHrs}.
 */
public interface IBioHrs {

    /**
     * Registra, modifica o elimina la plantilla de turno.
     *
     * @param acc        'I' | 'U' | 'D'
     * @param audUsuario usuario de auditoría (no es un campo del modelo)
     * @param motivo     por qué se hizo el cambio — queda en {@code tbio_bioBitacora},
     *                   no es un campo de la tabla (ver {@code sql/03_bitacora_biometrico.sql})
     */
    RespuestaSp registrar(BioHrs item, String acc, Long audUsuario, String motivo);

    /** [L] Plantillas de turno filtradas. */
    List<BioHrs> listar(Map<String, Object> filtro);
}
