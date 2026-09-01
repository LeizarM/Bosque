package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioBitacora;

import java.util.List;
import java.util.Map;

/**
 * Bitácora de cambios manuales — {@code tbio_bioBitacora}. Sólo lectura: nadie
 * la escribe desde la app, la escriben los {@code p_abm_Bio*} de Marcaciones
 * olvidadas y Horarios directamente.
 *
 * <p>SP: {@code p_list_BioBitacora}.
 */
public interface IBioBitacora {

    /**
     * [L] Entradas filtradas, más recientes primero.
     *
     * <p>Filtros esperados en el {@code Map}: {@code tabla} (p.ej.
     * {@code "BioHrEmpleado"}), {@code idRegistro} (historial de UNA fila
     * puntual), {@code desde}/{@code hasta} (ventana de fecha) — todos
     * opcionales y combinables.
     */
    List<BioBitacora> listar(Map<String, Object> filtro);
}
