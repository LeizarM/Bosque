package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Convocatoria;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class ConvocatoriaDao implements IConvocatoria {

    private static final String SP_ABM  = "p_abm_trs_Convocatoria";
    private static final String SP_LIST = "p_list_trs_Convocatoria";

    private final SpHelper spHelper;

    public ConvocatoriaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarConvocatoria(Convocatoria convocatoria, String acc) {
        log.info("Registrando Convocatoria: {}, Accion: {}", convocatoria.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, convocatoria, acc);
    }

    // ── L: la lista cruda de un sábado ────────────────────────────────────
    @Override
    public List<Convocatoria> obtenerConvocatorias(long idSabado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idSabado", idSabado);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Convocatoria.class);
    }

    // ── D: el detalle, con leTocabaPorRotacion ────────────────────────────
    @Override
    public List<Convocatoria> obtenerDetalleEvento(long idSabado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idSabado", idSabado);
        return spHelper.ejecutarListado(SP_LIST, filtro, "D", Convocatoria.class);
    }
}
