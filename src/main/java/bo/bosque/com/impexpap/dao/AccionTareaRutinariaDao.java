// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\AccionTareaRutinariaDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.AccionTareaRutinaria;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class AccionTareaRutinariaDao implements IAccionTareaRutinaria {

    private final SpHelper spHelper;

    public AccionTareaRutinariaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(AccionTareaRutinaria mb, String acc) {
        log.info("Registrando AccionTareaRutinaria: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_AccionTareaRutinaria", mb, acc);
    }

    @Override
    public List<AccionTareaRutinaria> listar(AccionTareaRutinaria filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_AccionTareaRutinaria", filtro, "L", AccionTareaRutinaria.class);
    }

    // FIX: usa Map (no el modelo) para que idATR==0 no viaje como filtro real —
    // el SP trata @idATR IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public AccionTareaRutinaria obtenerPorId(long idATR) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idATR", idATR);
        List<AccionTareaRutinaria> resultado = spHelper.ejecutarListado(
                "p_list_tac_AccionTareaRutinaria", filtro, "L", AccionTareaRutinaria.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
