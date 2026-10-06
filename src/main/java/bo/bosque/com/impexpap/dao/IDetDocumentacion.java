// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IDetDocumentacion.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DetDocumentacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IDetDocumentacion {

    /**
     * Registrar, actualizar o eliminar un detalle de documentación.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(DetDocumentacion mb, String acc);

    /**
     * [L] Listado de detalles de documentación con filtros opcionales (todos NULL = todas).
     */
    List<DetDocumentacion> listar(DetDocumentacion filtro);

    /**
     * [L] Carga UN detalle de documentación por ID exacto (para load-before-update).
     * Solo envía @idDetDoc al SP; todo lo demás queda en DEFAULT NULL.
     */
    DetDocumentacion obtenerPorId(long idDetDoc);
}
