// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IArqueoCajaSucursales.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ArqueoCajaSucursales;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IArqueoCajaSucursales {

    /**
     * Registrar, actualizar o eliminar un arqueo de caja de sucursales.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(ArqueoCajaSucursales mb, String acc);

    /**
     * [L] Listado de arqueos de caja de sucursales con filtros opcionales (todos NULL = todas).
     */
    List<ArqueoCajaSucursales> listar(ArqueoCajaSucursales filtro);

    /**
     * [L] Carga UN arqueo de caja de sucursales por ID exacto (para load-before-update).
     * Solo envía @idAC al SP; todo lo demás queda en DEFAULT NULL.
     */
    ArqueoCajaSucursales obtenerPorId(long idAC);

    /**
     * [L] El arqueo que se registró con una ocurrencia, o null si no tiene.
     * Para el PDF desde "Mis tareas", que conoce la tarea y no el arqueo. Desde
     * el archivo SQL 75 hay uno por ocurrencia; si quedara más de uno de antes,
     * el último.
     */
    ArqueoCajaSucursales obtenerPorOcurrencia(long idBitTarea);

    /**
     * [T] Tipo de cambio Hoy/Ayer — misma fuente real del legacy (servidor
     * enlazado SRV_2022, función CONEXION.dbo.fn_ObtenerTipoCambio). Lista
     * vacía si el servidor enlazado no está disponible (el SP lo tolera).
     */
    List<Map<String, Object>> obtenerTipoCambio();

    /**
     * [H] El arqueo anterior (más reciente, antes de hoy) de la sucursal de
     * la ocurrencia — para mostrar como contexto/comparación al crear uno
     * nuevo. Lista vacía si no hay ninguno previo (primer arqueo de esa
     * sucursal, o simplemente no hubo uno el día anterior).
     */
    List<Map<String, Object>> obtenerAnterior(long idBitTarea);

    /**
     * [B] Arqueos de un día para el panel de la revisión de Cierre de
     * Operaciones, enriquecidos con nombre de encargado/sucursal/tarea (mismo
     * join real del legacy). {@code todasSucursales}: replica el checkbox real
     * "Mostrar otras sucursales" (gated por el botón chkSuc en Flutter).
     *
     * @param fecha el día que se revisa; null = hoy (el de SQL Server). El
     *              parámetro existe desde el archivo SQL 60.
     */
    List<Map<String, Object>> obtenerDelDia(long idBitTarea, boolean todasSucursales, Date fecha);
}
