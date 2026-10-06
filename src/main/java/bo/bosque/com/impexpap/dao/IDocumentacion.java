// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IDocumentacion.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Documentacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IDocumentacion {

    /**
     * Registrar, actualizar o eliminar una Documentación.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(Documentacion mb, String acc);

    /**
     * [L] Listado de documentaciones con filtros opcionales (todos NULL = todas).
     */
    List<Documentacion> listar(Documentacion filtro);

    /**
     * [L] Carga UNA documentación por ID exacto (para load-before-update).
     * Solo envía @idDoc al SP; todo lo demás queda en DEFAULT NULL.
     */
    Documentacion obtenerPorId(long idDoc);
}
