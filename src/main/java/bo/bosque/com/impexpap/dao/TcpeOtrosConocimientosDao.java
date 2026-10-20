package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeOtrosConocimientos;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_otrosConocimientos} vía {@code p_list_tcpe_OtrosConocimientos} y {@code p_abm_tcpe_OtrosConocimientos}.
 */
@Repository
public class TcpeOtrosConocimientosDao extends TcpeCrudDao<TcpeOtrosConocimientos> implements ITcpeOtrosConocimientos {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codOtrosCono", "codConocimiento", "codPersonaPost" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codPersonaPost", "codConocimiento" };

    public TcpeOtrosConocimientosDao(SpHelper spHelper) {
        super(spHelper, TcpeOtrosConocimientos.class, "otrosConocimientos", "codOtrosCono", true, FILTROS, COLUMNAS);
    }
}
