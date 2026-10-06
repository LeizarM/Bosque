// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ICorte.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Corte;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ICorte {

    /**
     * Registrar, actualizar o eliminar un corte.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(Corte mb, String acc);

    /**
     * [L] Listado de cortes con filtros opcionales (todos NULL = todas).
     */
    List<Corte> listar(Corte filtro);

    /**
     * [L] Carga UN corte por ID exacto (para load-before-update).
     * Solo envía @idCorte al SP; todo lo demás queda en DEFAULT NULL.
     */
    Corte obtenerPorId(long idCorte);
}
