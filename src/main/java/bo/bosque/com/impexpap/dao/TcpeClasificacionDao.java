package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeClasificacion;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_clasificacion} vía {@code p_list_tcpe_Clasificacion} y {@code p_abm_tcpe_Clasificacion}.
 */
@Repository
public class TcpeClasificacionDao extends TcpeCrudDao<TcpeClasificacion> implements ITcpeClasificacion {

    /** Parámetros de p_list (PK, FK y activo); NULL = sin filtro. */
    private static final String[] FILTROS = { "codClasificacion", "codArea", "codCurso", "activo" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codArea", "codCurso", "nombre", "modalidad", "activo" };

    public TcpeClasificacionDao(SpHelper spHelper) {
        super(spHelper, TcpeClasificacion.class, "clasificacion", "codClasificacion", true, FILTROS, COLUMNAS);
    }
}
