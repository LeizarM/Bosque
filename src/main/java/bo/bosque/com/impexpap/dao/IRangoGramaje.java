package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.RangoGramajeDto;
import bo.bosque.com.impexpap.model.RangoGramaje;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Acceso al catalogo <b>tpr_RangoGramaje</b> (rangos de gramaje del papel, en g/m2).
 *
 * <p>Todo pasa por procedimientos almacenados: {@code p_abm_rangoGramaje} y
 * {@code p_list_rangoGramaje}. Nada de SQL crudo.
 *
 * <p><b>min y max son decimal(16,2)</b>, los unicos numericos exactos del modulo.
 * La interfaz los transporta como {@code java.math.BigDecimal} de punta a punta;
 * usar double o float aqui vuelve a perder los centavos que el codigo viejo
 * perdia.
 */
public interface IRangoGramaje {

    /**
     * Registra, actualiza o elimina un rango de gramaje.
     * Invoca {@code p_abm_rangoGramaje} con {@code @ACCION = acc}.
     *
     * @param rangoGramaje datos del rango; para 'U' y 'D' es obligatorio el idRangoGram
     * @param acc          'I' alta, 'U' modificacion, 'D' baja (fisica)
     * @return respuesta del SP. En el alta {@code getIdGenerado()} trae el
     *         idRangoGram nuevo (la columna es IDENTITY); en 'U' y 'D' devuelve
     *         el id afectado. Lanza SpBusinessException si el SP reporta un error
     *         de negocio (rango invalido, duplicado, o baja de un rango en uso).
     */
    RespuestaSp registrarRangoGramaje(RangoGramaje rangoGramaje, String acc);

    /**
     * Devuelve el catalogo completo de rangos, crudo.
     * Invoca {@code p_list_rangoGramaje} con {@code @ACCION = 'L'} y sin filtros.
     *
     * @return una fila por rango, ordenadas por limite inferior
     */
    List<RangoGramaje> listarRangoGramaje();

    /**
     * Devuelve un rango por su id.
     * Invoca {@code p_list_rangoGramaje} con {@code @ACCION = 'L'} filtrando por id.
     *
     * @param idRangoGram id del rango
     * @return el rango, o null si no existe
     */
    RangoGramaje obtenerPorId(Long idRangoGram);

    /**
     * Devuelve el catalogo de rangos con la etiqueta "[ min - max ]" ya armada,
     * para poblar combos y selectores.
     * Invoca {@code p_list_rangoGramaje} con {@code @ACCION = 'A'}.
     *
     * @return todos los rangos, ordenados por limite inferior
     */
    List<RangoGramajeDto> listarParaCombo();

    /**
     * Devuelve los rangos de gramaje configurados para un grupo de familia SAP y
     * un tipo de papel (formato), con la etiqueta ya armada.
     * Invoca {@code p_list_rangoGramaje} con {@code @ACCION = 'B'}, que cruza
     * tpr_grupoFamiliaSap, tpr_grupoFamTipoRangoGram y tpr_tipo.
     *
     * @param idGrpFamiliaSap grupo de familia SAP; obligatorio (esta rama no admite
     *                        "sin filtro": con null devuelve vacio)
     * @param idTipo          tipo de papel / formato; obligatorio por el mismo motivo
     * @return los rangos asignados a ese par, lista vacia si no hay configuracion
     */
    List<RangoGramajeDto> listarPorGrupoFamiliaYTipo(Long idGrpFamiliaSap, Long idTipo);
}
