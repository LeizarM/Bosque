package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeAreaPost;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_areaPost} vía {@code p_list_tcpe_AreaPost} y {@code p_abm_tcpe_AreaPost}.
 */
@Repository
public class TcpeAreaPostDao extends TcpeCrudDao<TcpeAreaPost> implements ITcpeAreaPost {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codAreaPost", "codTipo" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "nombArea", "codTipo" };

    public TcpeAreaPostDao(SpHelper spHelper) {
        super(spHelper, TcpeAreaPost.class, "areaPost", "codAreaPost", true, FILTROS, COLUMNAS);
    }
}
