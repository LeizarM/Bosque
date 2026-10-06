// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IVale.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Vale;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IVale {

    /**
     * Registrar, actualizar o eliminar un vale.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(Vale mb, String acc);

    /**
     * [L] Listado de vales con filtros opcionales (todos NULL = todas).
     */
    List<Vale> listar(Vale filtro);

    /**
     * [L] Carga UN vale por ID exacto (para load-before-update).
     * Solo envía @idVale al SP; todo lo demás queda en DEFAULT NULL.
     */
    Vale obtenerPorId(long idVale);
}
