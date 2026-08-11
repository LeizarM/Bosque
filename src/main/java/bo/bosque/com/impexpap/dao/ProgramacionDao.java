package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Programacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class ProgramacionDao implements IProgramacion {

    private static final String SP_ABM  = "p_abm_trs_Programacion";
    private static final String SP_LIST = "p_list_trs_Programacion";

    private final SpHelper spHelper;

    public ProgramacionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarProgramacion(Programacion programacion, String acc) {
        log.info("Registrando Programacion: {}, Accion: {}", programacion.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, programacion, acc);
    }

    // ── L: qué decidió cada jefe ──────────────────────────────────────────
    @Override
    public List<Programacion> obtenerProgramaciones(long idRol, long idSabado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        filtro.put("idSabado", idSabado);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Programacion.class);
    }

    // ── L: por id ─────────────────────────────────────────────────────────
    @Override
    public Programacion obtenerProgramacionPorId(long idProgramacion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idProgramacion", idProgramacion);
        List<Programacion> r = spHelper.ejecutarListado(SP_LIST, filtro, "L", Programacion.class);
        return r.isEmpty() ? null : r.get(0);
    }
}
