// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IMontoCajaChicaXSuc.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.MontoCajaChicaXSuc;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IMontoCajaChicaXSuc {

    /**
     * Registrar, actualizar o eliminar un monto de caja chica por sucursal.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(MontoCajaChicaXSuc mb, String acc);

    /**
     * [L] Listado de montos de caja chica por sucursal con filtros opcionales (todos NULL = todas).
     */
    List<MontoCajaChicaXSuc> listar(MontoCajaChicaXSuc filtro);

    /**
     * [L] Carga UN monto de caja chica por sucursal por ID exacto (para load-before-update).
     * Solo envía @idCS al SP; todo lo demás queda en DEFAULT NULL.
     */
    MontoCajaChicaXSuc obtenerPorId(long idCS);
}
