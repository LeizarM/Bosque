package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeCurso;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_curso} vía {@code p_list_tcpe_Curso} y {@code p_abm_tcpe_Curso}.
 */
@Repository
public class TcpeCursoDao extends TcpeCrudDao<TcpeCurso> implements ITcpeCurso {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codCurso", "codCapacitacion" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codCapacitacion", "inversion", "titulo", "descripcion", "lugar" };

    public TcpeCursoDao(SpHelper spHelper) {
        super(spHelper, TcpeCurso.class, "curso", "codCurso", true, FILTROS, COLUMNAS);
    }
}
