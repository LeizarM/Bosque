package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpePregunta;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_pregunta} vía {@code p_list_tcpe_Pregunta} y {@code p_abm_tcpe_Pregunta}.
 */
@Repository
public class TcpePreguntaDao extends TcpeCrudDao<TcpePregunta> implements ITcpePregunta {

    /** Parámetros de p_list (PK, FK y activo); NULL = sin filtro. */
    private static final String[] FILTROS = { "codPregunta", "codClasificacion", "activo" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codClasificacion", "prioridad", "pregunta", "activo" };

    public TcpePreguntaDao(SpHelper spHelper) {
        super(spHelper, TcpePregunta.class, "pregunta", "codPregunta", true, FILTROS, COLUMNAS);
    }
}
