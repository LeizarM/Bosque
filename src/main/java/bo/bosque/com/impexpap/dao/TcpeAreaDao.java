package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeArea;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_area} vía {@code p_list_tcpe_Area} y {@code p_abm_tcpe_Area}.
 */
@Repository
public class TcpeAreaDao extends TcpeCrudDao<TcpeArea> implements ITcpeArea {

    /** Parámetros de p_list (PK, FK y activo); NULL = sin filtro. */
    private static final String[] FILTROS = { "codArea", "activo" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "nombreArea", "descripArea", "activo" };

    public TcpeAreaDao(SpHelper spHelper) {
        super(spHelper, TcpeArea.class, "area", "codArea", true, FILTROS, COLUMNAS);
    }
}
