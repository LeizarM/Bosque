// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IMovCaja.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.MovCaja;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IMovCaja {

    /**
     * Registrar, actualizar o eliminar un movimiento de caja.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(MovCaja mb, String acc);

    /**
     * [L] Listado de movimientos de caja con filtros opcionales (todos NULL = todas).
     */
    List<MovCaja> listar(MovCaja filtro);

    /**
     * [L] Carga UN movimiento de caja por ID exacto (para load-before-update).
     * Solo envía @idMC al SP; todo lo demás queda en DEFAULT NULL.
     */
    MovCaja obtenerPorId(long idMC);
}
