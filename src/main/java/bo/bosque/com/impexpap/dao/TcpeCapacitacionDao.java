package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeCapacitacion;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_capacitacion} vía {@code p_list_tcpe_Capacitacion} y {@code p_abm_tcpe_Capacitacion}.
 */
@Repository
public class TcpeCapacitacionDao extends TcpeCrudDao<TcpeCapacitacion> implements ITcpeCapacitacion {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codCapacitacion" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "esInterna", "titulo", "descripcion", "objetivo", "telfReferencia", "destinatarios", "proFechaInicio", "proFechaFin", "fechaInicio", "fechaFin" };

    public TcpeCapacitacionDao(SpHelper spHelper) {
        super(spHelper, TcpeCapacitacion.class, "capacitacion", "codCapacitacion", true, FILTROS, COLUMNAS);
    }
}
