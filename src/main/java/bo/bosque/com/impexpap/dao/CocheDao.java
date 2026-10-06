// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\CocheDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Coche;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class CocheDao implements ICoche {

    private final SpHelper spHelper;

    public CocheDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(Coche mb, String acc) {
        log.info("Registrando Coche: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_Coche", mb, acc);
    }

    @Override
    public List<Coche> listar(Coche filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_Coche", filtro, "L", Coche.class);
    }

    // FIX: usa Map (no el modelo) para que idCoche==0 no viaje como filtro real —
    // el SP trata @idCoche IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public Coche obtenerPorId(long idCoche) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCoche", idCoche);
        List<Coche> resultado = spHelper.ejecutarListado(
                "p_list_tac_Coche", filtro, "L", Coche.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
