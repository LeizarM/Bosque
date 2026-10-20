package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeUsuario;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_usuario} vía {@code p_list_tcpe_Usuario} y {@code p_abm_tcpe_Usuario}.
 */
@Repository
public class TcpeUsuarioDao extends TcpeCrudDao<TcpeUsuario> implements ITcpeUsuario {

    /** Parámetros de p_list (PK, FK y activo); NULL = sin filtro. */
    private static final String[] FILTROS = { "codUsuario", "codTipoUsuario", "codArea", "codEmpleado", "activo" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codEmpleado", "codTipoUsuario", "ci", "codArea", "nombres", "apellidoPaterno", "apellidoMaterno", "login", "activo" };

    public TcpeUsuarioDao(SpHelper spHelper) {
        super(spHelper, TcpeUsuario.class, "usuario", "codUsuario", true, FILTROS, COLUMNAS);
    }
}
