package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Asignacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class AsignacionDao implements IAsignacion {

    private static final String SP_ABM  = "p_abm_trs_Asignacion";
    private static final String SP_LIST = "p_list_trs_Asignacion";

    private final SpHelper spHelper;

    public AsignacionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarAsignacion(Asignacion asignacion, String acc) {
        log.info("Registrando Asignacion: {}, Accion: {}", asignacion.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, asignacion, acc);
    }

    // ── L: las celdas en plano ────────────────────────────────────────────
    @Override
    public List<Asignacion> obtenerAsignaciones(long idRol, long idSabado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        filtro.put("idSabado", idSabado);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Asignacion.class);
    }

    // ── L: por id ─────────────────────────────────────────────────────────
    @Override
    public Asignacion obtenerAsignacionPorId(long idAsignacion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idAsignacion", idAsignacion);
        List<Asignacion> r = spHelper.ejecutarListado(SP_LIST, filtro, "L", Asignacion.class);
        return r.isEmpty() ? null : r.get(0);
    }

    // ── M: la mezcla por capa y estado ────────────────────────────────────
    // Son agregados, pero el modelo ya tiene los campos (queEs, celdas...), asi
    // que se mapea igual que el resto: los campos que la consulta no trae quedan
    // en su default y no molestan.
    @Override
    public List<Asignacion> obtenerMezclaPorOrigen(long idRol) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        return spHelper.ejecutarListado(SP_LIST, filtro, "M", Asignacion.class);
    }

    // ── A: cuanto resuelve el sistema solo (una sola fila) ─────────────────
    @Override
    public Asignacion obtenerAutomatizacion(long idRol) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        List<Asignacion> r = spHelper.ejecutarListado(SP_LIST, filtro, "A", Asignacion.class);
        return r.isEmpty() ? null : r.get(0);
    }
}
