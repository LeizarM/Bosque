package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeTipoUsuario;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_tipoUsuario} vía {@code p_list_tcpe_TipoUsuario} y {@code p_abm_tcpe_TipoUsuario}.
 */
@Repository
public class TcpeTipoUsuarioDao extends TcpeCrudDao<TcpeTipoUsuario> implements ITcpeTipoUsuario {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codTipoUsuario" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "tipo" };

    public TcpeTipoUsuarioDao(SpHelper spHelper) {
        super(spHelper, TcpeTipoUsuario.class, "tipoUsuario", "codTipoUsuario", true, FILTROS, COLUMNAS);
    }
}
