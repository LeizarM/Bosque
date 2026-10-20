package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeDpto;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_dpto} vía {@code p_list_tcpe_Dpto} y {@code p_abm_tcpe_Dpto}.
 */
@Repository
public class TcpeDptoDao extends TcpeCrudDao<TcpeDpto> implements ITcpeDpto {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codDpto" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "departamento" };

    public TcpeDptoDao(SpHelper spHelper) {
        super(spHelper, TcpeDpto.class, "dpto", "codDpto", true, FILTROS, COLUMNAS);
    }
}
