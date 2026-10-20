package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeSeleccionFase;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_seleccionFase} vía {@code p_list_tcpe_SeleccionFase} y {@code p_abm_tcpe_SeleccionFase}.
 */
@Repository
public class TcpeSeleccionFaseDao extends TcpeCrudDao<TcpeSeleccionFase> implements ITcpeSeleccionFase {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codSeleccionFase", "codConvocatoria", "codFase" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codConvocatoria", "codFase" };

    public TcpeSeleccionFaseDao(SpHelper spHelper) {
        super(spHelper, TcpeSeleccionFase.class, "seleccionFase", "codSeleccionFase", true, FILTROS, COLUMNAS);
    }
}
