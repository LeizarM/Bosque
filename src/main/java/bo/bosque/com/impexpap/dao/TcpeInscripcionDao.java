package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeInscripcion;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_inscripcion} vía {@code p_list_tcpe_Inscripcion} y {@code p_abm_tcpe_Inscripcion}.
 */
@Repository
public class TcpeInscripcionDao extends TcpeCrudDao<TcpeInscripcion> implements ITcpeInscripcion {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codInscripcion", "codCapacitacion", "codUsuario" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codCapacitacion", "codUsuario", "observaciones", "fechaInscrip" };

    public TcpeInscripcionDao(SpHelper spHelper) {
        super(spHelper, TcpeInscripcion.class, "inscripcion", "codInscripcion", true, FILTROS, COLUMNAS);
    }
}
