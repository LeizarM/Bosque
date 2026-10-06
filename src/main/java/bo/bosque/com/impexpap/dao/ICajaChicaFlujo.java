// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ICajaChicaFlujo.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.RegistrarEgresoCajaChicaRequest;
import bo.bosque.com.impexpap.model.CajaChica;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

public interface ICajaChicaFlujo {
    List<CajaChica> listarDelLote(long idBitTarea, long audUsuario);

    RespuestaSp registrarEgreso(RegistrarEgresoCajaChicaRequest req, long audUsuario);

    RespuestaSp finalizar(long idBitTarea, long audUsuario);

    /**
     * [C] Cierra el lote VIGENTE de la sucursal del cargo actual del
     * empleado dueño de {@code idBitTarea} y abre el siguiente (MAX(lote)+1)
     * con su saldo inicial ya sembrado desde tac_montoCajaChicaXSuc — mismo
     * mecanismo de siembra que {@link #listarDelLote}. Reemplaza al legacy
     * "Generar PDF" (que además rotaba el lote) sin la parte de reporte,
     * migrada aparte. Acción independiente de {@link #finalizar}: no cierra
     * la bitácora.
     */
    RespuestaSp cerrarLote(long idBitTarea, long audUsuario);

    /**
     * [H] "Ver Cajas Chicas" del legacy: histórico de lotes de la sucursal
     * de esta ocurrencia — lote, desde/hasta, total egresos.
     */
    List<Map<String, Object>> obtenerHistorialLotes(long idBitTarea);
}
