// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ISucXMovCaja.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SucXMovCaja;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

public interface ISucXMovCaja {

    /**
     * Registrar, actualizar o eliminar una sucursal por movimiento de caja.
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrar(SucXMovCaja mb, String acc);

    /**
     * [L] Listado de sucursales por movimiento de caja con filtros opcionales (todos NULL = todas).
     */
    List<SucXMovCaja> listar(SucXMovCaja filtro);

    /**
     * [L] Carga UNA sucursal por movimiento de caja por ID exacto (para load-before-update).
     * Solo envía @idSxMC al SP; todo lo demás queda en DEFAULT NULL.
     */
    SucXMovCaja obtenerPorId(long idSxMC);

    /**
     * [A] Desglose de saldo SAP por caja de la sucursal de la ocurrencia
     * ({@code idBitTarea} — la sucursal se resuelve server-side del cargo
     * vigente del empleado dueño de esa ocurrencia). Reemplaza el manual-entry
     * que tenía Arqueo de Caja — misma consulta real del legacy
     * (v_SAP_movCajaSap join tac_sucXMovCaja). Forma dinámica: no es el CRUD
     * estándar de la tabla, es un JOIN de despliegue.
     */
    List<Map<String, Object>> listarSaldoSapPorOcurrencia(long idBitTarea);
}
