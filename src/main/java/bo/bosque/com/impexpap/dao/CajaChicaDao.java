// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\CajaChicaDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CajaChica;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class CajaChicaDao implements ICajaChica {

    private final SpHelper spHelper;

    public CajaChicaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(CajaChica mb, String acc) {
        log.info("Registrando CajaChica: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_CajaChica", mb, acc);
    }

    @Override
    public List<CajaChica> listar(CajaChica filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_CajaChica", filtro, "L", CajaChica.class);
    }

    // FIX: usa Map (no el modelo) para que idCC==0 no viaje como filtro real —
    // el SP trata @idCC IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public CajaChica obtenerPorId(long idCC) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCC", idCC);
        List<CajaChica> resultado = spHelper.ejecutarListado(
                "p_list_tac_CajaChica", filtro, "L", CajaChica.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
