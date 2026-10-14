// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\CajaChicaFlujoDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.RegistrarEgresoCajaChicaRequest;
import bo.bosque.com.impexpap.model.CajaChica;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO del flujo especial "Caja Chica" (idATR=7) — reemplaza dlgCajaChica de
 * WizardTareas.java. La lógica vive como ACCION='D'/'R'/'C'/'H'/'V' de los
 * procs genéricos p_list_tac_CajaChica / p_abm_tac_CajaChica. Desde el archivo
 * SQL 96 no hay ocurrencia: se manda {@code codEmpleado} y no
 * {@code idBitTarRuti}.
 */
@Slf4j
@Repository
public class CajaChicaFlujoDao implements ICajaChicaFlujo {

    private final SpHelper spHelper;

    public CajaChicaFlujoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<CajaChica> listarDelLote(long codEmpleado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("codEmpleado", codEmpleado);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarListado("p_list_tac_CajaChica", params, "D", CajaChica.class);
    }

    @Override
    public RespuestaSp registrarEgreso(RegistrarEgresoCajaChicaRequest req, long codEmpleado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("codEmpleado", codEmpleado);
        params.put("montoEg", req.getMontoEg());
        params.put("descripcion", req.getDescripcion());
        params.put("codEmpDestino", req.getCodEmpDestino());
        params.put("numFactura", req.getNumFactura());
        params.put("numVale", req.getNumVale());
        params.put("moneda", req.getMoneda() != null ? req.getMoneda() : "BS");
        if (req.getFecha() != null) params.put("fecha", req.getFecha());  // Archivo SQL 100
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_CajaChica", params, "R");
    }

    @Override
    public RespuestaSp cerrarLote(long codEmpleado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("codEmpleado", codEmpleado);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_CajaChica", params, "C");
    }

    @Override
    public List<Map<String, Object>> obtenerHistorialLotes(long codEmpleado) {
        Map<String, Object> params = new HashMap<>();
        params.put("codEmpleado", codEmpleado);
        params.put("ACCION", "H");
        return spHelper.ejecutarListadoDinamico("p_list_tac_CajaChica", params);
    }

    @Override
    public List<Map<String, Object>> listarTalonarios(long codEmpleado) {
        Map<String, Object> params = new HashMap<>();
        params.put("codEmpleado", codEmpleado);
        params.put("ACCION", "V");
        return spHelper.ejecutarListadoDinamico("p_list_tac_CajaChica", params);
    }

    @Override
    public List<Map<String, Object>> reglasRecibo(long codEmpleado) {
        Map<String, Object> params = new HashMap<>();
        params.put("codEmpleado", codEmpleado);
        params.put("ACCION", "G");
        return spHelper.ejecutarListadoDinamico("p_list_tac_CajaChica", params);
    }
}
