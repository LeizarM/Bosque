package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeReqAcademico;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_reqAcademico} vía {@code p_list_tcpe_ReqAcademico} y {@code p_abm_tcpe_ReqAcademico}.
 */
@Repository
public class TcpeReqAcademicoDao extends TcpeCrudDao<TcpeReqAcademico> implements ITcpeReqAcademico {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codReqAcademico", "codConvocatoria", "codFormacion" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codConvocatoria", "codFormacion" };

    public TcpeReqAcademicoDao(SpHelper spHelper) {
        super(spHelper, TcpeReqAcademico.class, "reqAcademico", "codReqAcademico", true, FILTROS, COLUMNAS);
    }
}
