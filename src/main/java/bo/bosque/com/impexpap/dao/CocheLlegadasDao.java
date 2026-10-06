// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\CocheLlegadasDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CocheLlegadas;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class CocheLlegadasDao implements ICocheLlegadas {

    private final SpHelper spHelper;

    public CocheLlegadasDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(CocheLlegadas mb, String acc) {
        log.info("Registrando CocheLlegadas: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_CocheLlegadas", mb, acc);
    }

    @Override
    public List<CocheLlegadas> listar(CocheLlegadas filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_CocheLlegadas", filtro, "L", CocheLlegadas.class);
    }

    // FIX: usa Map (no el modelo) para que idCo==0 no viaje como filtro real —
    // el SP trata @idCo IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public CocheLlegadas obtenerPorId(long idCo) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCo", idCo);
        List<CocheLlegadas> resultado = spHelper.ejecutarListado(
                "p_list_tac_CocheLlegadas", filtro, "L", CocheLlegadas.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
