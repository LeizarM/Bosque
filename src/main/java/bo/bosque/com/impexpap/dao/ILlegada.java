// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ILlegada.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Llegada;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ILlegada {

    /**
     * Registrar, actualizar o eliminar una llegada.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(Llegada mb, String acc);

    /**
     * [L] Listado de llegadas con filtros opcionales (todos NULL = todas).
     */
    List<Llegada> listar(Llegada filtro);

    /**
     * [L] Carga UNA llegada por ID exacto (para load-before-update).
     * Solo envía @idRp al SP; todo lo demás queda en DEFAULT NULL.
     */
    Llegada obtenerPorId(long idRp);

    /**
     * [B] Llegadas de un día para el panel de la revisión de Cierre de
     * Operaciones, enriquecidas con nombre de sucursal. {@code todasSucursales}:
     * replica el checkbox real "Mostrar otras sucursales" del legacy (gated por
     * chkSuc en Flutter).
     *
     * @param fecha el día que se revisa; null = hoy (el de SQL Server). El
     *              parámetro existe desde el archivo SQL 60.
     */
    List<Map<String, Object>> obtenerDelDia(long idBitTarea, boolean todasSucursales, Date fecha);
}
