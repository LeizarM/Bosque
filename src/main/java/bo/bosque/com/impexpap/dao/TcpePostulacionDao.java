package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpePostulacion;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_postulacion} vía {@code p_list_tcpe_Postulacion} y {@code p_abm_tcpe_Postulacion}.
 * La PK no es IDENTITY: sin código el SP asigna el siguiente; con código {@code guardar} decide I/U según exista.
 */
@Repository
public class TcpePostulacionDao extends TcpeCrudDao<TcpePostulacion> implements ITcpePostulacion {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codPostulacion", "codConvocatoria", "codPersonaPost" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codPersonaPost", "codConvocatoria", "fechaPost" };

    public TcpePostulacionDao(SpHelper spHelper) {
        super(spHelper, TcpePostulacion.class, "postulacion", "codPostulacion", false, FILTROS, COLUMNAS);
    }
}
