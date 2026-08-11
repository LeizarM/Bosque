package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.EstadoTurno;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class EstadoTurnoDao implements IEstadoTurno {

    private static final String SP_ABM  = "p_abm_trs_EstadoTurno";
    private static final String SP_LIST = "p_list_trs_EstadoTurno";

    private final SpHelper spHelper;

    public EstadoTurnoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarEstadoTurno(EstadoTurno estadoTurno, String acc) {
        log.info("Registrando EstadoTurno: {}, Accion: {}", estadoTurno.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, estadoTurno, acc);
    }

    // ── L: catálogo completo ──────────────────────────────────────────────
    @Override
    public List<EstadoTurno> obtenerEstadosTurno() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "L", EstadoTurno.class);
    }

    // ── L: por código del Excel ───────────────────────────────────────────
    // Map y no el modelo: el SP filtra con (ISNULL(@id,0)=0 OR ...), así que
    // mandar el modelo entero con idEstadoTurno=0 traería todo el catálogo.
    @Override
    public EstadoTurno obtenerPorCodigo(String codigoExcel) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codigoExcel", codigoExcel);
        List<EstadoTurno> r = spHelper.ejecutarListado(SP_LIST, filtro, "L", EstadoTurno.class);
        return r.isEmpty() ? null : r.get(0);
    }
}
