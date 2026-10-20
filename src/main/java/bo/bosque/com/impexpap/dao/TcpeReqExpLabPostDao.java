package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeReqExpLabPost;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_reqExpLabPost} vía {@code p_list_tcpe_ReqExpLabPost} y {@code p_abm_tcpe_ReqExpLabPost}.
 */
@Repository
public class TcpeReqExpLabPostDao extends TcpeCrudDao<TcpeReqExpLabPost> implements ITcpeReqExpLabPost {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codExpLabPost", "codPersonaPost" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codPersonaPost", "nombreEmpresa", "cargo", "descripcion", "fechaFin", "fechaInicio" };

    public TcpeReqExpLabPostDao(SpHelper spHelper) {
        super(spHelper, TcpeReqExpLabPost.class, "reqExpLabPost", "codExpLabPost", true, FILTROS, COLUMNAS);
    }
}
