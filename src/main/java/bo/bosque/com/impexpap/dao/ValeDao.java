// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ValeDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Vale;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class ValeDao implements IVale {

    private final SpHelper spHelper;

    public ValeDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(Vale mb, String acc) {
        log.info("Registrando Vale: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_Vale", mb, acc);
    }

    @Override
    public List<Vale> listar(Vale filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_Vale", filtro, "L", Vale.class);
    }

    // FIX: usa Map (no el modelo) para que idVale==0 no viaje como filtro real —
    // el SP trata @idVale IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public Vale obtenerPorId(long idVale) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idVale", idVale);
        List<Vale> resultado = spHelper.ejecutarListado(
                "p_list_tac_Vale", filtro, "L", Vale.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
