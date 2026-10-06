// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\MovCajaDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.MovCaja;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class MovCajaDao implements IMovCaja {

    private final SpHelper spHelper;

    public MovCajaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(MovCaja mb, String acc) {
        log.info("Registrando MovCaja: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_MovCaja", mb, acc);
    }

    @Override
    public List<MovCaja> listar(MovCaja filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_MovCaja", filtro, "L", MovCaja.class);
    }

    // FIX: usa Map (no el modelo) para que idMC==0 no viaje como filtro real —
    // el SP trata @idMC IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public MovCaja obtenerPorId(long idMC) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idMC", idMC);
        List<MovCaja> resultado = spHelper.ejecutarListado(
                "p_list_tac_MovCaja", filtro, "L", MovCaja.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }
}
