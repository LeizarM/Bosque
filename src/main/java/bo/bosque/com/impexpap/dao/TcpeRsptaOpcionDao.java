package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeRsptaOpcion;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_rsptaOpcion} vía {@code p_list_tcpe_RsptaOpcion} y {@code p_abm_tcpe_RsptaOpcion}.
 */
@Repository
public class TcpeRsptaOpcionDao extends TcpeCrudDao<TcpeRsptaOpcion> implements ITcpeRsptaOpcion {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codRsptaOp", "codRespuesta" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codRespuesta", "opcionRspta", "esValido" };

    public TcpeRsptaOpcionDao(SpHelper spHelper) {
        super(spHelper, TcpeRsptaOpcion.class, "rsptaOpcion", "codRsptaOp", true, FILTROS, COLUMNAS);
    }
}
