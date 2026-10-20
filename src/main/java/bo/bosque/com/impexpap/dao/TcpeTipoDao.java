package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeTipo;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_tipo} vía {@code p_list_tcpe_Tipo} y {@code p_abm_tcpe_Tipo}.
 */
@Repository
public class TcpeTipoDao extends TcpeCrudDao<TcpeTipo> implements ITcpeTipo {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codTipo" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "nombre", "esVigente", "idGrupo" };

    public TcpeTipoDao(SpHelper spHelper) {
        super(spHelper, TcpeTipo.class, "tipo", "codTipo", true, FILTROS, COLUMNAS);
    }
}
