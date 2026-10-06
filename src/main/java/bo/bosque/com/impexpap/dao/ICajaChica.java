// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ICajaChica.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CajaChica;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ICajaChica {

    /**
     * Registrar, actualizar o eliminar una caja chica.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(CajaChica mb, String acc);

    /**
     * [L] Listado de cajas chicas con filtros opcionales (todos NULL = todas).
     */
    List<CajaChica> listar(CajaChica filtro);

    /**
     * [L] Carga UNA caja chica por ID exacto (para load-before-update).
     * Solo envía @idCC al SP; todo lo demás queda en DEFAULT NULL.
     */
    CajaChica obtenerPorId(long idCC);
}
