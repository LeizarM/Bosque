// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\CorteDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Corte;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class CorteDao implements ICorte {

    private final SpHelper spHelper;

    public CorteDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(Corte mb, String acc) {
        log.info("Registrando Corte: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_Corte", mb, acc);
    }

    @Override
    public List<Corte> listar(Corte filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_Corte", filtro, "L", Corte.class);
    }

    // FIX: usa Map (no el modelo) para que idCorte==0 no viaje como filtro real —
    // el SP trata @idCorte IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public Corte obtenerPorId(long idCorte) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCorte", idCorte);
        List<Corte> resultado = spHelper.ejecutarListado(
                "p_list_tac_Corte", filtro, "L", Corte.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
