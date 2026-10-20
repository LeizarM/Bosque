package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeRegistroExamen;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_registroExamen} vía {@code p_list_tcpe_RegistroExamen} y {@code p_abm_tcpe_RegistroExamen}.
 */
@Repository
public class TcpeRegistroExamenDao extends TcpeCrudDao<TcpeRegistroExamen> implements ITcpeRegistroExamen {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codRegExamen", "codExamen", "codRsptaOpcion" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codExamen", "codRsptaOpcion" };

    public TcpeRegistroExamenDao(SpHelper spHelper) {
        super(spHelper, TcpeRegistroExamen.class, "registroExamen", "codRegExamen", true, FILTROS, COLUMNAS);
    }
}
