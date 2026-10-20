package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpePreguntaH;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_preguntaH} vía {@code p_list_tcpe_PreguntaH} y {@code p_abm_tcpe_PreguntaH}.
 */
@Repository
public class TcpePreguntaHDao extends TcpeCrudDao<TcpePreguntaH> implements ITcpePreguntaH {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codPreguntaH", "codExamen" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codPregunta", "codClasificacion", "codExamen", "pregunta" };

    public TcpePreguntaHDao(SpHelper spHelper) {
        super(spHelper, TcpePreguntaH.class, "preguntaH", "codPreguntaH", true, FILTROS, COLUMNAS);
    }
}
