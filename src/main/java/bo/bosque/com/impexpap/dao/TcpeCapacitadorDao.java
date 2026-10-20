package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpeCapacitador;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_capacitador} vía {@code p_list_tcpe_Capacitador} y {@code p_abm_tcpe_Capacitador}.
 */
@Repository
public class TcpeCapacitadorDao extends TcpeCrudDao<TcpeCapacitador> implements ITcpeCapacitador {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codCapacitador" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "sexo", "telefonos", "ci", "nombre", "apPaterno", "apMaterno", "direccion", "email", "pais", "fechaNacimiento" };

    public TcpeCapacitadorDao(SpHelper spHelper) {
        super(spHelper, TcpeCapacitador.class, "capacitador", "codCapacitador", true, FILTROS, COLUMNAS);
    }
}
