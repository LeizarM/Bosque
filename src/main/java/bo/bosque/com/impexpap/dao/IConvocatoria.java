package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Convocatoria;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * La lista nominal de un evento.
 * SPs: {@code p_abm_trs_Convocatoria} · {@code p_list_trs_Convocatoria}.
 */
public interface IConvocatoria {

    /**
     * Convoca o excusa a UNA persona para el sábado de un evento, y rehace las
     * celdas de ese día. Sólo funciona en sábados con
     * {@code alcanceEvento IS NOT NULL}: las ausencias de un sábado normal van
     * por {@code trs_sp_programar}.
     *
     * <p>'I' y 'U' hacen lo mismo (borran y reinsertan), porque alternar
     * CONVOCADO ↔ EXCUSADO chocaría contra el UNIQUE. 'D' lo saca de la lista y
     * devuelve a esa persona a la rotación.
     *
     * <p>Para convocar en BLOQUE —todo el subárbol de un jefe, o todos los de un
     * cargo— está {@code trs_sp_convocar}.
     *
     * @param convocatoria Datos de la convocatoria
     * @param acc          'I' | 'U' | 'D'
     */
    RespuestaSp registrarConvocatoria(Convocatoria convocatoria, String acc);

    /** [L] La lista cruda de un sábado. */
    List<Convocatoria> obtenerConvocatorias(long idSabado);

    /**
     * [D] El detalle del evento con el contraste contra la rotación.
     * {@code leTocabaPorRotacion} es la columna clave: un CONVOCADO con 1 ya venía
     * solo y esa fila no agrega a nadie.
     */
    List<Convocatoria> obtenerDetalleEvento(long idSabado);
}
