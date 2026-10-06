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
 * WizardTareas.java. La lógica vive como ACCION='D'/'R'/'F' de los procs
 * genéricos p_list_tac_CajaChica / p_abm_tac_CajaChica (no en procs con
 * nombre propio — ver
 * sql/2026-09-03_tac_tareaRutinaria_24_flujos_especiales_sobre_procs_genericos.sql).
 */
@Slf4j
@Repository
public class CajaChicaFlujoDao implements ICajaChicaFlujo {

    private final SpHelper spHelper;

    public CajaChicaFlujoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<CajaChica> listarDelLote(long idBitTarea, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarRuti", idBitTarea);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarListado("p_list_tac_CajaChica", params, "D", CajaChica.class);
    }

    @Override
    public RespuestaSp registrarEgreso(RegistrarEgresoCajaChicaRequest req, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarRuti", req.getIdBitTarea());
        params.put("montoEg", req.getMontoEg());
        params.put("descripcion", req.getDescripcion());
        params.put("codEmpDestino", req.getCodEmpDestino());
        params.put("numFactura", req.getNumFactura());
        params.put("numVale", req.getNumVale());
        params.put("moneda", req.getMoneda() != null ? req.getMoneda() : "BS");
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_CajaChica", params, "R");
    }

    @Override
    public RespuestaSp finalizar(long idBitTarea, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarRuti", idBitTarea);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_CajaChica", params, "F");
    }

    @Override
    public RespuestaSp cerrarLote(long idBitTarea, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarRuti", idBitTarea);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_CajaChica", params, "C");
    }

    @Override
    public List<Map<String, Object>> obtenerHistorialLotes(long idBitTarea) {
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarRuti", idBitTarea);
        params.put("ACCION", "H");
        return spHelper.ejecutarListadoDinamico("p_list_tac_CajaChica", params);
    }
}
