package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeRsptaOpcionH;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_rsptaOpcionH} vía {@code p_list_tcpe_RsptaOpcionH} y {@code p_abm_tcpe_RsptaOpcionH}.
 */
@Repository
public class TcpeRsptaOpcionHDao extends TcpeCrudDao<TcpeRsptaOpcionH> implements ITcpeRsptaOpcionH {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codRsptaOpH", "codRespuestaH" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codRespuestaH", "codRespuesta", "codRsptaOp", "opcionRspta", "esValido" };

    public TcpeRsptaOpcionHDao(SpHelper spHelper) {
        super(spHelper, TcpeRsptaOpcionH.class, "rsptaOpcionH", "codRsptaOpH", true, FILTROS, COLUMNAS);
    }
}
