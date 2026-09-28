package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.GrupoFamTipoRangoGramDto;
import bo.bosque.com.impexpap.model.GrupoFamTipoRangoGram;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Acceso a la tabla puente <b>tpr_grupoFamTipoRangoGram</b> (grupo de familia SAP
 * x tipo de papel -> rango de gramaje).
 *
 * <p>Todo pasa por procedimientos almacenados:
 * {@code p_abm_grupoFamTipoRangoGram} y {@code p_list_grupoFamTipoRangoGram}.
 *
 * <p><b>Sobre los tipos de los ids:</b> las columnas idGrpFamiliaSap e idTipo son
 * {@code int} en esta tabla pero {@code bigint} en sus tablas padre
 * (tpr_grupoFamiliaSap, tpr_tipo). Los metodos de esta interfaz reciben
 * {@code Long} porque ese es el tipo con el que el resto de la aplicacion
 * transporta esos ids; el DAO hace el estrechamiento a int en un solo lugar.
 */
public interface IGrupoFamTipoRangoGram {

    /**
     * Registra, actualiza o elimina una asignacion de rango de gramaje.
     * Invoca {@code p_abm_grupoFamTipoRangoGram}.
     *
     * <p>La fila se identifica por su clave natural {@code (idGrpFamiliaSap, idTipo)}:
     * la tabla es un HEAP sin PK ni IDENTITY, asi que {@code RespuestaSp.getIdGenerado()}
     * siempre vuelve en 0 aun despues de un alta exitosa.
     *
     * @param grupoFamTipoRangoGram datos de la fila; para 'U' y 'D' basta con la clave
     *                              natural (en 'U' ademas el idRangoGram nuevo)
     * @param acc                   'I' alta, 'U' modificacion, 'D' baja
     * @return respuesta del SP; lanza SpBusinessException si el SP reporta error de negocio
     */
    RespuestaSp registrarGrupoFamTipoRangoGram(GrupoFamTipoRangoGram grupoFamTipoRangoGram, String acc);

    /**
     * Devuelve todas las asignaciones cargadas. Invoca {@code p_list_grupoFamTipoRangoGram}
     * con {@code @ACCION = 'L'} y sin filtros.
     *
     * @return las filas crudas de la tabla puente (hoy 61)
     */
    List<GrupoFamTipoRangoGram> listarGrupoFamTipoRangoGram();

    /**
     * Devuelve las asignaciones de un grupo de familia SAP (una por tipo de papel).
     * Invoca {@code p_list_grupoFamTipoRangoGram} con {@code @ACCION = 'L'} filtrando
     * por grupo.
     *
     * @param idGrpFamiliaSap id del grupo de familia; null o 0 equivale a "sin filtro"
     * @return las filas del grupo, lista vacia si no tiene ninguna
     */
    List<GrupoFamTipoRangoGram> obtenerPorGrupoFamilia(Long idGrpFamiliaSap);

    /**
     * Devuelve la unica fila que corresponde a la clave natural del registro.
     * Invoca {@code p_list_grupoFamTipoRangoGram} con {@code @ACCION = 'L'} filtrando
     * por grupo y tipo.
     *
     * @param idGrpFamiliaSap parte 1 de la clave natural
     * @param idTipo          parte 2 de la clave natural
     * @return la fila, o null si ese par todavia no esta configurado
     */
    GrupoFamTipoRangoGram obtenerPorClaveNatural(Long idGrpFamiliaSap, Long idTipo);

    /**
     * Devuelve la tabla de parametros de gramaje ya pivoteada: un renglon por grupo
     * de familia SAP con sus columnas Liviano / Mediano / Pesado.
     * Invoca {@code p_list_grupoFamTipoRangoGram} con {@code @ACCION = 'A'}.
     *
     * @return una fila por grupo de familia, incluidos los que aun no tienen rangos
     *         asignados (llegan con las tres columnas en null)
     */
    List<GrupoFamTipoRangoGramDto> listarParametrosGramaje();
}
