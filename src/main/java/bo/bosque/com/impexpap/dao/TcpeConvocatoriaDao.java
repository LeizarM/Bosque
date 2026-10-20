package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeConvocatoria;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_convocatoria} vía {@code p_list_tcpe_Convocatoria} y {@code p_abm_tcpe_Convocatoria}.
 */
@Repository
public class TcpeConvocatoriaDao extends TcpeCrudDao<TcpeConvocatoria> implements ITcpeConvocatoria {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codConvocatoria", "codDpto", "codArea" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "codArea", "codDpto", "gestion", "esInterna", "titulo", "observacion", "descripcion", "nombrePuesto", "competencias", "objCargo", "funciones", "estado", "proFechaInicio", "proFechaFin", "fechaInicio", "fechaFin" };

    public TcpeConvocatoriaDao(SpHelper spHelper) {
        super(spHelper, TcpeConvocatoria.class, "convocatoria", "codConvocatoria", true, FILTROS, COLUMNAS);
    }
}
