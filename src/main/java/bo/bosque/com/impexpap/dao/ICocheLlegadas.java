// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ICocheLlegadas.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CocheLlegadas;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ICocheLlegadas {

    /**
     * Registrar, actualizar o eliminar una llegada de coche.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(CocheLlegadas mb, String acc);

    /**
     * [L] Listado de llegadas de coche con filtros opcionales (todos NULL = todas).
     */
    List<CocheLlegadas> listar(CocheLlegadas filtro);

    /**
     * [L] Carga UNA llegada de coche por ID exacto (para load-before-update).
     * Solo envía @idCo al SP; todo lo demás queda en DEFAULT NULL.
     */
    CocheLlegadas obtenerPorId(long idCo);
}
