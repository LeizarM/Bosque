package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeExperienciaLab;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_experienciaLab} vía {@code p_list_tcpe_ExperienciaLab} y {@code p_abm_tcpe_ExperienciaLab}.
 */
@Repository
public class TcpeExperienciaLabDao extends TcpeCrudDao<TcpeExperienciaLab> implements ITcpeExperienciaLab {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codExpLab", "codConvocatoria" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codConvocatoria", "cantidad", "medida", "ocupacion" };

    public TcpeExperienciaLabDao(SpHelper spHelper) {
        super(spHelper, TcpeExperienciaLab.class, "experienciaLab", "codExpLab", true, FILTROS, COLUMNAS);
    }
}
