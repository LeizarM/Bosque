// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ICoche.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Coche;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ICoche {

    /**
     * Registrar, actualizar o eliminar un coche.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(Coche mb, String acc);

    /**
     * [L] Listado de coches con filtros opcionales (todos NULL = todas).
     */
    List<Coche> listar(Coche filtro);

    /**
     * [S] Listado para la pantalla de gestion: igual que {@link #listar} pero con
     * {@code nombreSucursal}. A diferencia de {@code listar}, un {@code estado = 0}
     * SI filtra (coches inactivos); {@code null} o texto en blanco no filtran.
     */
    List<Coche> listarGestion(Coche filtro);

    /**
     * [L] Carga UN coche por ID exacto (para load-before-update).
     * Solo envía @idCoche al SP; todo lo demás queda en DEFAULT NULL.
     */
    Coche obtenerPorId(long idCoche);
}
