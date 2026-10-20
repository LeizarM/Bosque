package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeConocimientosPost;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_conocimientosPost} vía {@code p_list_tcpe_ConocimientosPost} y {@code p_abm_tcpe_ConocimientosPost}.
 * La PK no es IDENTITY: sin código el SP asigna el siguiente; con código {@code guardar} decide I/U según exista.
 */
@Repository
public class TcpeConocimientosPostDao extends TcpeCrudDao<TcpeConocimientosPost> implements ITcpeConocimientosPost {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codConocimiento" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "nombreConocimiento", "esVigente" };

    public TcpeConocimientosPostDao(SpHelper spHelper) {
        super(spHelper, TcpeConocimientosPost.class, "conocimientosPost", "codConocimiento", false, FILTROS, COLUMNAS);
    }
}
