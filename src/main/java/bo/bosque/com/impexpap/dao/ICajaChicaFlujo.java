// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ICajaChicaFlujo.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.RegistrarEgresoCajaChicaRequest;
import bo.bosque.com.impexpap.model.CajaChica;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

/**
 * Caja Chica sin ocurrencia (archivo SQL 96): todo va por {@code codEmpleado},
 * el del token, y el SP saca la sucursal de su cargo vigente.
 */
public interface ICajaChicaFlujo {
    List<CajaChica> listarDelLote(long codEmpleado, long audUsuario);

    RespuestaSp registrarEgreso(RegistrarEgresoCajaChicaRequest req, long codEmpleado, long audUsuario);

    /**
     * [C] Cierra el lote VIGENTE de la sucursal y abre el siguiente
     * (MAX(lote)+1) con su saldo inicial ya sembrado desde
     * tac_montoCajaChicaXSuc — mismo mecanismo de siembra que
     * {@link #listarDelLote}. Reemplaza al legacy "Generar PDF" (que además
     * rotaba el lote) sin la parte de reporte.
     */
    RespuestaSp cerrarLote(long codEmpleado, long audUsuario);

    /**
     * [H] "Ver Cajas Chicas" del legacy: histórico de lotes de la sucursal
     * — lote, codSucursal, desde/hasta, total egresos.
     */
    List<Map<String, Object>> obtenerHistorialLotes(long codEmpleado);

    /**
     * [V] Talonarios de vales de caja chica entregados a la sucursal (o a la
     * persona): rango, usados, libres y el vale que sigue (archivo SQL 93).
     * Vacío = la sucursal no tiene talonario.
     */
    List<Map<String, Object>> listarTalonarios(long codEmpleado);

    /**
     * [G] Las reglas del número de recibo en palabras (titulo, regla,
     * reciboObligatorio), armadas por el SP con su configuración (archivo
     * SQL 99). Una fila.
     */
    List<Map<String, Object>> reglasRecibo(long codEmpleado);
}
