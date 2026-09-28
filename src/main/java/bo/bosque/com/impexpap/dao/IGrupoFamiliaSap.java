package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.GrupoFamiliaSap;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Contrato de acceso a datos de <b>tpr_grupoFamiliaSap</b>.
 * Todo pasa por los procedimientos p_abm_grupoFamiliaSap y p_list_grupoFamiliaSap.
 */
public interface IGrupoFamiliaSap {

    /**
     * Inserta un grupo de familia SAP. Invoca p_abm_grupoFamiliaSap con ACCION 'I'.
     *
     * @param mb datos del grupo (idGrpFamiliaSap se ignora, es IDENTITY)
     * @return respuesta del SP; {@code getIdGenerado()} trae el id recien creado
     */
    RespuestaSp registrar(GrupoFamiliaSap mb);

    /**
     * Actualiza un grupo de familia SAP. Invoca p_abm_grupoFamiliaSap con ACCION 'U'.
     *
     * @param mb datos del grupo, con idGrpFamiliaSap obligatorio
     * @return respuesta del SP
     */
    RespuestaSp actualizar(GrupoFamiliaSap mb);

    /**
     * Elimina un grupo de familia SAP. Invoca p_abm_grupoFamiliaSap con ACCION 'D'.
     *
     * @param idGrpFamiliaSap id del grupo a eliminar
     * @return respuesta del SP
     */
    RespuestaSp eliminar(Long idGrpFamiliaSap);

    /**
     * Lista todos los grupos de familia SAP. Invoca p_list_grupoFamiliaSap con ACCION 'L'.
     *
     * @return todas las filas de tpr_grupoFamiliaSap
     */
    List<GrupoFamiliaSap> listar();

    /**
     * Obtiene un grupo por su id. Invoca p_list_grupoFamiliaSap con ACCION 'L'
     * enviando unicamente @idGrpFamiliaSap.
     *
     * @param idGrpFamiliaSap id buscado
     * @return lista con cero o una fila
     */
    List<GrupoFamiliaSap> obtenerPorId(Long idGrpFamiliaSap);

    /**
     * Lista filtrando por los campos no nulos del modelo recibido.
     * Invoca p_list_grupoFamiliaSap con ACCION 'L'.
     *
     * @param filtro campos en null = sin filtro
     * @return filas que cumplen el filtro
     */
    List<GrupoFamiliaSap> buscar(GrupoFamiliaSap filtro);
}
