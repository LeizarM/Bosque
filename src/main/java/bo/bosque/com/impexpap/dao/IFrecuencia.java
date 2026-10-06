// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IFrecuencia.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Frecuencia;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IFrecuencia {

    /**
     * Registrar, actualizar o eliminar una frecuencia.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(Frecuencia mb, String acc);

    /**
     * [L] Listado de frecuencias con filtros opcionales (todos NULL = todas).
     */
    List<Frecuencia> listar(Frecuencia filtro);

    /**
     * [L] Carga UNA frecuencia por ID exacto (para load-before-update).
     * Solo envía @idFrec al SP; todo lo demás queda en DEFAULT NULL.
     */
    Frecuencia obtenerPorId(long idFrec);
}
