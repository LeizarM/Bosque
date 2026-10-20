package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeRespuesta;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_respuesta} vía {@code p_list_tcpe_Respuesta} y {@code p_abm_tcpe_Respuesta}.
 */
@Repository
public class TcpeRespuestaDao extends TcpeCrudDao<TcpeRespuesta> implements ITcpeRespuesta {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codRespuesta", "codPregunta" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codPregunta", "prioridad", "respuesta", "esCerrada" };

    public TcpeRespuestaDao(SpHelper spHelper) {
        super(spHelper, TcpeRespuesta.class, "respuesta", "codRespuesta", true, FILTROS, COLUMNAS);
    }
}
