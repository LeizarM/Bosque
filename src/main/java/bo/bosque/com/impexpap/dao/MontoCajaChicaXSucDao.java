// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\MontoCajaChicaXSucDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.MontoCajaChicaXSuc;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class MontoCajaChicaXSucDao implements IMontoCajaChicaXSuc {

    private final SpHelper spHelper;

    public MontoCajaChicaXSucDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(MontoCajaChicaXSuc mb, String acc) {
        log.info("Registrando MontoCajaChicaXSuc: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_MontoCajaChicaXSuc", mb, acc);
    }

    @Override
    public List<MontoCajaChicaXSuc> listar(MontoCajaChicaXSuc filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_MontoCajaChicaXSuc", filtro, "L", MontoCajaChicaXSuc.class);
    }

    // FIX: usa Map (no el modelo) para que idCS==0 no viaje como filtro real —
    // el SP trata @idCS IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public MontoCajaChicaXSuc obtenerPorId(long idCS) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCS", idCS);
        List<MontoCajaChicaXSuc> resultado = spHelper.ejecutarListado(
                "p_list_tac_MontoCajaChicaXSuc", filtro, "L", MontoCajaChicaXSuc.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
