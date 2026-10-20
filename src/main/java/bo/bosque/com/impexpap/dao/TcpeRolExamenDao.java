package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeRolExamen;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_rolExamen} vía {@code p_list_tcpe_RolExamen} y {@code p_abm_tcpe_RolExamen}.
 */
@Repository
public class TcpeRolExamenDao extends TcpeCrudDao<TcpeRolExamen> implements ITcpeRolExamen {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codRolExamen", "codArea", "codCapacitacion", "codConvocatoria", "codClasificacion" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codCapacitacion", "codConvocatoria", "codArea", "ponderacion", "puntos", "duracionMinutos", "cantidadPreg", "cantExamenGenerar", "ubicacion", "modalidad", "esRolCapacitacion", "fecha", "horaInicio", "horaFin", "codClasificacion", "titulo", "observaciones", "notaAprobacion", "mostrarRespuestas" };

    public TcpeRolExamenDao(SpHelper spHelper) {
        super(spHelper, TcpeRolExamen.class, "rolExamen", "codRolExamen", true, FILTROS, COLUMNAS);
    }
}
