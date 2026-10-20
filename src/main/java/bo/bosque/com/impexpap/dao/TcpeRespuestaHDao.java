package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeRespuestaH;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_respuestaH} vía {@code p_list_tcpe_RespuestaH} y {@code p_abm_tcpe_RespuestaH}.
 */
@Repository
public class TcpeRespuestaHDao extends TcpeCrudDao<TcpeRespuestaH> implements ITcpeRespuestaH {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codRespuestaH", "codPreguntaH" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codPreguntaH", "codPregunta", "codRespuesta", "respuesta", "respuestaU", "esCerrada", "esCorrecta" };

    public TcpeRespuestaHDao(SpHelper spHelper) {
        super(spHelper, TcpeRespuestaH.class, "respuestaH", "codRespuestaH", true, FILTROS, COLUMNAS);
    }
}
