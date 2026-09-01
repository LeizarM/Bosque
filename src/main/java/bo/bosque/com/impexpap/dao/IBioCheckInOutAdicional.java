package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioCheckInOutAdicional;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

/**
 * Marcaciones olvidadas / corregidas manualmente — {@code tbio_bioCHECKINOUTAdicinal}.
 *
 * <p>SPs: {@code p_abm_BioCHECKINOUTAdicinal} · {@code p_list_BioCHECKINOUTAdicinal}.
 */
public interface IBioCheckInOutAdicional {

    /**
     * Registra, modifica o elimina la marcación adicional. El SP también
     * soporta {@code ACCION='A'} (confirma la marcación adicional en
     * {@code tbio_bioCHECKINOUT}, con guard contra duplicados).
     *
     * <p>{@code idGenerado} devuelve {@code USERID} en el alta (clave
     * compuesta, sin IDENTITY).
     *
     * @param acc        'I' | 'U' | 'D' | 'A'
     * @param audUsuario usuario de auditoría (no es un campo del modelo)
     * @param motivo     por qué se registró/editó/borró la marca — queda en
     *                   {@code tbio_bioBitacora}, no es un campo de la tabla
     *                   (ver {@code sql/03_bitacora_biometrico.sql})
     */
    RespuestaSp registrar(BioCheckInOutAdicional item, String acc, Long audUsuario, String motivo);

    /** [L] Marcaciones adicionales filtradas. */
    List<BioCheckInOutAdicional> listar(Map<String, Object> filtro);
}
