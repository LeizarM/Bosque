package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeConocimientos;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_conocimientos} vía {@code p_list_tcpe_Conocimientos} y {@code p_abm_tcpe_Conocimientos}.
 */
@Repository
public class TcpeConocimientosDao extends TcpeCrudDao<TcpeConocimientos> implements ITcpeConocimientos {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codConocimiento", "codConvocatoria" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codConvocatoria", "grado", "conocimiento" };

    public TcpeConocimientosDao(SpHelper spHelper) {
        super(spHelper, TcpeConocimientos.class, "conocimientos", "codConocimiento", true, FILTROS, COLUMNAS);
    }
}
