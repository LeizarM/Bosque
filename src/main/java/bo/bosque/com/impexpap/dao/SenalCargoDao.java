// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\SenalCargoDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.SenalCargoDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@code p_abm_tac_SenalCargo} y {@code p_list_tac_SenalCargo} (archivo SQL 82). */
@Repository
public class SenalCargoDao implements ISenalCargo {

    private static final String SP_ABM = "p_abm_tac_SenalCargo";
    private static final String SP_LIST = "p_list_tac_SenalCargo";

    private final SpHelper spHelper;

    public SenalCargoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<SenalCargoDto> listar(boolean verCalladas) {
        Map<String, Object> params = new HashMap<>();
        params.put("verCalladas", verCalladas ? 1 : 0);
        return spHelper.ejecutarListado(SP_LIST, params, "L", SenalCargoDto.class);
    }

    @Override
    public RespuestaSp estaBien(long codEmpleado, long idTarRuti, String obs, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("codEmpleado", codEmpleado);
        params.put("idTarRuti", idTarRuti);
        params.put("obs", obs);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "S");
    }

    @Override
    public RespuestaSp volverAMostrar(long idSC, long codEmpleado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idSC", idSC);
        params.put("codEmpleado", codEmpleado);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }
}
