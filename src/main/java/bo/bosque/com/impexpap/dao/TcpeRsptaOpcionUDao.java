package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeRsptaOpcionU;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_rsptaOpcionU} vía {@code p_list_tcpe_RsptaOpcionU} y {@code p_abm_tcpe_RsptaOpcionU}.
 */
@Repository
public class TcpeRsptaOpcionUDao extends TcpeCrudDao<TcpeRsptaOpcionU> implements ITcpeRsptaOpcionU {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codRsptaOpU", "codRsptaOpH", "codExamen" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codRsptaOpH", "codExamen", "codRespuesta", "codRsptaOp", "opcionRspta", "esValido" };

    public TcpeRsptaOpcionUDao(SpHelper spHelper) {
        super(spHelper, TcpeRsptaOpcionU.class, "rsptaOpcionU", "codRsptaOpU", true, FILTROS, COLUMNAS);
    }
}
