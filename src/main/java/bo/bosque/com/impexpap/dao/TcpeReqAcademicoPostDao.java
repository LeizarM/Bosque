package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeReqAcademicoPost;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_reqAcademicoPost} vía {@code p_list_tcpe_ReqAcademicoPost} y {@code p_abm_tcpe_ReqAcademicoPost}.
 */
@Repository
public class TcpeReqAcademicoPostDao extends TcpeCrudDao<TcpeReqAcademicoPost> implements ITcpeReqAcademicoPost {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codReqAcaPost", "codFormacion", "codPersonaPost" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codPersonaPost", "codFormacion", "gestion", "nomInstitucion" };

    public TcpeReqAcademicoPostDao(SpHelper spHelper) {
        super(spHelper, TcpeReqAcademicoPost.class, "reqAcademicoPost", "codReqAcaPost", true, FILTROS, COLUMNAS);
    }
}
