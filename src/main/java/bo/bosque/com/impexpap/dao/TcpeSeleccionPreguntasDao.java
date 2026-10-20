package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeSeleccionPreguntas;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_seleccionPreguntas} vía {@code p_list_tcpe_SeleccionPreguntas} y {@code p_abm_tcpe_SeleccionPreguntas}.
 */
@Repository
public class TcpeSeleccionPreguntasDao extends TcpeCrudDao<TcpeSeleccionPreguntas> implements ITcpeSeleccionPreguntas {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codSeleccionPreguntas", "codExamen", "codPregunta" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codExamen", "codPregunta" };

    public TcpeSeleccionPreguntasDao(SpHelper spHelper) {
        super(spHelper, TcpeSeleccionPreguntas.class, "seleccionPreguntas", "codSeleccionPreguntas", true, FILTROS, COLUMNAS);
    }
}
