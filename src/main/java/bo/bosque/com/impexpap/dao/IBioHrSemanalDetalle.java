package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioHrSemanalDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

/**
 * Día → turno dentro de un horario semanal — {@code tbio_bioHrSemanalDetalle}.
 *
 * <p>SPs: {@code p_abm_BioHrSemanalDetalle} · {@code p_list_BioHrSemanalDetalle}.
 */
public interface IBioHrSemanalDetalle {

    /**
     * Registra, modifica o elimina el detalle del día. El SP también soporta
     * {@code ACCION='E'} (borra todo el detalle de un {@code idHrSemanal} y
     * reinserta 7 filas en blanco, una por día, para editar de cero).
     *
     * @param acc        'I' | 'U' | 'D' | 'E'
     * @param audUsuario usuario de auditoría (no es un campo del modelo)
     * @param motivo     por qué se hizo el cambio — queda en {@code tbio_bioBitacora},
     *                   no es un campo de la tabla (ver {@code sql/03_bitacora_biometrico.sql})
     */
    RespuestaSp registrar(BioHrSemanalDetalle item, String acc, Long audUsuario, String motivo);

    /** [L] Detalle filtrado (por {@code idHrSemanal}, por día, etc.). */
    List<BioHrSemanalDetalle> listar(Map<String, Object> filtro);
}
