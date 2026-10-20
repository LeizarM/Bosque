package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeExamen;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_examen} vía {@code p_list_tcpe_Examen} y {@code p_abm_tcpe_Examen}.
 */
@Repository
public class TcpeExamenDao extends TcpeCrudDao<TcpeExamen> implements ITcpeExamen {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codExamen", "codPostulacion", "codRolExamen", "codUsuario" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codRolExamen", "codUsuario", "codPostulacion", "esPostulacion", "resultado", "observaciones", "titulo", "fechaInicio", "fechaFin", "preguntaActual", "ultimaActividad", "salidasPestana", "ultimaSalida" };

    public TcpeExamenDao(SpHelper spHelper) {
        super(spHelper, TcpeExamen.class, "examen", "codExamen", true, FILTROS, COLUMNAS);
    }
}
