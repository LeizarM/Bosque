package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.GrupoFamiliaSap;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * DAO legacy de tpr_grupoFamiliaSap. <b>No usar en codigo nuevo: usar
 * {@link GrupoFamiliaSapDao}.</b>
 *
 * <p>Su version anterior armaba SQL crudo
 * (<code>jdbcTemplate.update("execute @idGrpFamiliaSap=?, ...")</code>, sin nombre
 * de procedimiento, por lo que siempre fallaba con BadSqlGrammarException y
 * devolvia false silenciosamente) y mandaba los codigos SAP como int contra
 * columnas varchar. Eso viola la regla de "nunca SQL crudo", asi que quedo solo
 * como puente para cualquier llamador viejo mientras se migra.
 *
 * <p>Ya no es un bean de Spring ni implementa {@code IGrupoFamiliaSap}: tener dos
 * {@code @Repository} sobre la misma interfaz hacia ambigua la inyeccion por tipo.
 * No tiene ningun consumidor en el repositorio; <b>este archivo se puede borrar</b>
 * (no lo elimino yo porque la operacion de borrado quedo fuera de mis permisos).
 *
 * @deprecated usar {@link GrupoFamiliaSapDao}.
 */
@Deprecated
public class GrupoFamiliaDao {

    private final GrupoFamiliaSapDao delegado;

    public GrupoFamiliaDao(GrupoFamiliaSapDao delegado) {
        this.delegado = delegado;
    }

    /**
     * Puente con la firma vieja. Delega en el DAO nuevo, que va por
     * p_abm_grupoFamiliaSap.
     *
     * @param grupoFamiliaSap datos del grupo
     * @param acc             'I', 'U' o 'D'
     * @return true si el SP no devolvio error
     * @deprecated usar {@link GrupoFamiliaSapDao#registrar(GrupoFamiliaSap)},
     *             {@link GrupoFamiliaSapDao#actualizar(GrupoFamiliaSap)} o
     *             {@link GrupoFamiliaSapDao#eliminar(Long)}.
     */
    @Deprecated
    public boolean registrarGrupoFamiliaSap(GrupoFamiliaSap grupoFamiliaSap, String acc) {
        final RespuestaSp resp;
        if ("I".equals(acc)) {
            resp = delegado.registrar(grupoFamiliaSap);
        } else if ("U".equals(acc)) {
            resp = delegado.actualizar(grupoFamiliaSap);
        } else if ("D".equals(acc)) {
            resp = delegado.eliminar(grupoFamiliaSap.getIdGrpFamiliaSap());
        } else {
            throw new IllegalArgumentException("ACCION no soportada por p_abm_grupoFamiliaSap: " + acc);
        }
        return resp.isExitoso();
    }
}
