package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioCheckInOut;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Marcaciones crudas del biométrico — {@code tbio_bioCHECKINOUT}.
 *
 * <p>SPs: {@code p_abm_BioCHECKINOUT} · {@code p_list_BioCHECKINOUT}.
 */
public interface IBioCheckInOut {

    /**
     * {@code p_abm_BioCHECKINOUT} sólo implementa {@code ACCION='B'} — no es un ABM
     * real (no hay ramas I/U/D). Esa acción no inserta esta fila: cuenta las
     * marcaciones del mes de {@code checkTime} y dispara
     * {@code p_SAP_Rpt_Biometrico ACCION='F'} (la importación desde el dispositivo),
     * devolviendo un texto de diagnóstico (conteo antes/después) por el parámetro
     * OUTPUT {@code @RETORNA}. No usar {@code SpHelper.ejecutarAbm}: ese SP no
     * declara {@code @error/@errormsg/@idGenerado}.
     *
     * @param checkTime fecha que determina el mes a reimportar
     * @return el texto de {@code @RETORNA}
     */
    String dispararImportacionMensual(Date checkTime);

    /** [L] Marcaciones filtradas (por {@code USERID}, rango de {@code CHECKTIME}, etc.). */
    List<BioCheckInOut> listar(Map<String, Object> filtro);
}
