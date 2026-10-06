// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IDetArqueoCajaSucursales.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DetArqueoCajaSucursales;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IDetArqueoCajaSucursales {

    /**
     * Registrar, actualizar o eliminar un detalle de arqueo de caja de sucursales.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(DetArqueoCajaSucursales mb, String acc);

    /**
     * [L] Listado de detalles de arqueo de caja de sucursales con filtros opcionales (todos NULL = todas).
     */
    List<DetArqueoCajaSucursales> listar(DetArqueoCajaSucursales filtro);

    /**
     * [L] Carga UN detalle de arqueo de caja de sucursales por ID exacto (para load-before-update).
     * Solo envía @idDetAS al SP; todo lo demás queda en DEFAULT NULL.
     */
    DetArqueoCajaSucursales obtenerPorId(long idDetAS);
}
