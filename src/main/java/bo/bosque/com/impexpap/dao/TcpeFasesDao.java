package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeFases;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_fases} vía {@code p_list_tcpe_Fases} y {@code p_abm_tcpe_Fases}.
 */
@Repository
public class TcpeFasesDao extends TcpeCrudDao<TcpeFases> implements ITcpeFases {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codFase" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "nombre" };

    public TcpeFasesDao(SpHelper spHelper) {
        super(spHelper, TcpeFases.class, "fases", "codFase", true, FILTROS, COLUMNAS);
    }
}
