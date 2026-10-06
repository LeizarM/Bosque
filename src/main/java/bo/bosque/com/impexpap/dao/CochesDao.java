// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\CochesDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CocheDelDia;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO del flujo especial "Coches" (idATR=6) — reemplaza el diálogo dlgCoches
 * de WizardTareas.java. La lógica vive como ACCION='D'/'M' de los procs
 * genéricos p_list_tac_CocheLlegadas / p_abm_tac_CocheLlegadas (no en procs
 * con nombre propio — ver
 * sql/2026-09-03_tac_tareaRutinaria_24_flujos_especiales_sobre_procs_genericos.sql).
 */
@Slf4j
@Repository
public class CochesDao implements ICoches {

    private final SpHelper spHelper;

    public CochesDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<CocheDelDia> listarDelDia(long idTarRuti, long idBitTarea, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idTarRuti", idTarRuti);
        params.put("idBitTarRuti", idBitTarea);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarListado("p_list_tac_CocheLlegadas", params, "D", CocheDelDia.class);
    }

    @Override
    public RespuestaSp marcarLlegada(long idCo, int llego, String obs, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idCo", idCo);
        params.put("llego", llego);
        params.put("obs", obs);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_CocheLlegadas", params, "M");
    }
}
