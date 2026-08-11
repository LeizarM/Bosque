package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.DiaNoLaborableDto;
import bo.bosque.com.impexpap.model.Sabado;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class SabadoDao implements ISabado {

    private static final String SP_ABM  = "p_abm_trs_Sabado";
    private static final String SP_LIST = "p_list_trs_Sabado";

    private final SpHelper spHelper;

    public SabadoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarSabado(Sabado sabado, String acc) {
        log.info("Registrando Sabado: {}, Accion: {}", sabado.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, sabado, acc);
    }

    // ── L: los sábados de un rol ──────────────────────────────────────────
    @Override
    public List<Sabado> obtenerSabados(long idRol) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Sabado.class);
    }

    // ── L: por id ─────────────────────────────────────────────────────────
    @Override
    public Sabado obtenerSabadoPorId(long idSabado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idSabado", idSabado);
        List<Sabado> r = spHelper.ejecutarListado(SP_LIST, filtro, "L", Sabado.class);
        return r.isEmpty() ? null : r.get(0);
    }

    // ── C: cobertura (el SUBTOTAL del Excel) ──────────────────────────────
    @Override
    public List<Sabado> obtenerCobertura(long idRol, int mes) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        filtro.put("mes", mes);
        return spHelper.ejecutarListado(SP_LIST, filtro, "C", Sabado.class);
    }

    // ── F: feriados desincronizados (el caso del puente) ──────────────────
    @Override
    public List<Sabado> obtenerFeriadosDesincronizados(long idRol) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        return spHelper.ejecutarListado(SP_LIST, filtro, "F", Sabado.class);
    }

    // ── N: los días no laborables de RR.HH. ───────────────────────────────
    // Lee trh_diaNoLaborable, que es otra tabla: mapea contra el DTO, no contra Sabado.
    @Override
    public List<DiaNoLaborableDto> obtenerDiasNoLaborables(int anio, long codSucursal) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("anio", anio);
        filtro.put("codSucursal", codSucursal);
        return spHelper.ejecutarListado(SP_LIST, filtro, "N", DiaNoLaborableDto.class);
    }
}
