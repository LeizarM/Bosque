package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeFormacion;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_formacion} vía {@code p_list_tcpe_Formacion} y {@code p_abm_tcpe_Formacion}.
 */
@Repository
public class TcpeFormacionDao extends TcpeCrudDao<TcpeFormacion> implements ITcpeFormacion {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codFormacion" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "nombreGrado", "grado" };

    public TcpeFormacionDao(SpHelper spHelper) {
        super(spHelper, TcpeFormacion.class, "formacion", "codFormacion", true, FILTROS, COLUMNAS);
    }
}
