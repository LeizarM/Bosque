// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\FrecuenciaDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Frecuencia;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class FrecuenciaDao implements IFrecuencia {

    private final SpHelper spHelper;

    public FrecuenciaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(Frecuencia mb, String acc) {
        log.info("Registrando Frecuencia: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_Frecuencia", mb, acc);
    }

    @Override
    public List<Frecuencia> listar(Frecuencia filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_Frecuencia", filtro, "L", Frecuencia.class);
    }

    // FIX: usa Map (no el modelo) para que idFrec==0 no viaje como filtro real —
    // el SP trata @idFrec IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public Frecuencia obtenerPorId(long idFrec) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idFrec", idFrec);
        List<Frecuencia> resultado = spHelper.ejecutarListado(
                "p_list_tac_Frecuencia", filtro, "L", Frecuencia.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
