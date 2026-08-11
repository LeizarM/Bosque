package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Cambio;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class CambioDao implements ICambio {

    private static final String SP_ABM  = "p_abm_trs_Cambio";
    private static final String SP_LIST = "p_list_trs_Cambio";

    private final SpHelper spHelper;

    public CambioDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarCambio(Cambio cambio, String acc) {
        log.info("Registrando Cambio: {}, Accion: {}", cambio.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, cambio, acc);
    }

    // ── A: aprobar y aplicar a la grilla ──────────────────────────────────
    // Va por ejecutarAbmMap y no por ejecutarAbm(modelo) a proposito: el SP
    // solo necesita el id y quien aprueba, y mandarle el modelo entero le
    // pasaria los ceros por default de Java en campos que no queremos tocar.
    @Override
    public RespuestaSp aprobarCambio(long idCambio, long codAprobador, long audUsuario) {
        log.info("Aprobando Cambio: {}, Aprobador: {}", idCambio, codAprobador);
        Map<String, Object> params = new HashMap<>();
        params.put("idCambio", idCambio);
        params.put("codAprobador", codAprobador);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "A");
    }

    // ── L: las permutas y coberturas ──────────────────────────────────────
    @Override
    public List<Cambio> obtenerCambios(long idRol, String estado) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idRol", idRol);
        if (estado != null && !estado.trim().isEmpty()) {
            filtro.put("estado", estado);
        }
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Cambio.class);
    }

    // ── L: por id ─────────────────────────────────────────────────────────
    @Override
    public Cambio obtenerCambioPorId(long idCambio) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCambio", idCambio);
        List<Cambio> r = spHelper.ejecutarListado(SP_LIST, filtro, "L", Cambio.class);
        return r.isEmpty() ? null : r.get(0);
    }
}
