// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\TareaRutinariaDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TareaRutinaria;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class TareaRutinariaDao implements ITareaRutinaria {

    private final SpHelper spHelper;

    public TareaRutinariaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(TareaRutinaria mb, String acc) {
        log.info("Registrando TareaRutinaria: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_TareaRutinaria", mb, acc);
    }

    @Override
    public List<TareaRutinaria> listar(TareaRutinaria filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_TareaRutinaria", filtro, "L", TareaRutinaria.class);
    }

    // FIX: usa Map (no el modelo) para que idTarRuti==0 no viaje como filtro real —
    // el SP trata @idTarRuti IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public TareaRutinaria obtenerPorId(long idTarRuti) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTarRuti", idTarRuti);
        List<TareaRutinaria> resultado = spHelper.ejecutarListado(
                "p_list_tac_TareaRutinaria", filtro, "L", TareaRutinaria.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
