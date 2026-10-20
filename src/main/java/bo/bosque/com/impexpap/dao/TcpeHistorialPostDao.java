package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeHistorialPost;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_historialPost} vía {@code p_list_tcpe_HistorialPost} y {@code p_abm_tcpe_HistorialPost}.
 */
@Repository
public class TcpeHistorialPostDao extends TcpeCrudDao<TcpeHistorialPost> implements ITcpeHistorialPost {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codHistorialPost", "codFase", "codPostulacion" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codFase", "codPostulacion", "fechaHistorial" };

    public TcpeHistorialPostDao(SpHelper spHelper) {
        super(spHelper, TcpeHistorialPost.class, "historialPost", "codHistorialPost", true, FILTROS, COLUMNAS);
    }
}
