package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Cambio;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Permutas y coberturas: el caso "no puedo ese sábado". Nace SOLICITADO, no
 * toca la grilla, y sólo la mueve cuando RR.HH. o el jefe lo aprueba.
 *
 * <p>SPs: {@code p_abm_trs_Cambio} · {@code p_list_trs_Cambio}.
 */
public interface ICambio {

    /**
     * Registra la solicitud, la modifica o la ANULA.
     *
     * <p>'I' nace en {@code SOLICITADO} y <b>no escribe ninguna celda</b> — para eso
     * está {@link #aprobarCambio}. Una {@code PERMUTA} exige
     * {@code idSabadoReposicion} (el sábado que se devuelve); una {@code COBERTURA} no.
     *
     * <p>'U' y 'D' rebotan si el cambio ya está APROBADO: sus celdas ya se
     * escribieron y deshacerlas por acá dejaría la grilla mintiendo. Ese caso se
     * corrige celda por celda con {@code trs_sp_corregirCelda}.
     *
     * @param cambio Datos del cambio
     * @param acc    'I' | 'U' | 'D'
     */
    RespuestaSp registrarCambio(Cambio cambio, String acc);

    /**
     * [A] Aprueba y APLICA. Es lo único que mueve la grilla, y son tres
     * escrituras que van juntas en una transacción: el titular pasa a 'C', el que
     * cubre aparece con '1' en un sábado que no le tocaba, y si hay reposición se
     * le libera ese otro sábado.
     *
     * <p>Revalida contra el estado de HOY, no contra el de la solicitud: si el
     * titular o el reemplazo ya no son participantes activos, rebota.
     *
     * @param idCambio     El cambio a aprobar
     * @param codAprobador Quién aprueba
     * @param audUsuario   Usuario de auditoría
     */
    RespuestaSp aprobarCambio(long idCambio, long codAprobador, long audUsuario);

    /**
     * [L] Las permutas y coberturas con los nombres ya resueltos.
     * @param idRol  0 = todos
     * @param estado null o vacío = todos los estados
     */
    List<Cambio> obtenerCambios(long idRol, String estado);

    /** [L] Un cambio por su id. */
    Cambio obtenerCambioPorId(long idCambio);
}
