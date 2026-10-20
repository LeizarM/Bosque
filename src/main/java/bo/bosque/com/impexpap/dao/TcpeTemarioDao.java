package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeTemario;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_temario} vía {@code p_list_tcpe_Temario} y {@code p_abm_tcpe_Temario}.
 */
@Repository
public class TcpeTemarioDao extends TcpeCrudDao<TcpeTemario> implements ITcpeTemario {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codTemario", "codCapacitador", "codCurso" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codCurso", "codCapacitador", "nombre", "descripcion" };

    public TcpeTemarioDao(SpHelper spHelper) {
        super(spHelper, TcpeTemario.class, "temario", "codTemario", true, FILTROS, COLUMNAS);
    }
}
