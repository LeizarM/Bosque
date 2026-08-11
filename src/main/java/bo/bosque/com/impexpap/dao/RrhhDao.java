package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Rrhh;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class RrhhDao implements IRrhh {

    private static final String SP_ABM  = "p_abm_trs_Rrhh";
    private static final String SP_LIST = "p_list_trs_Rrhh";

    private final SpHelper spHelper;

    public RrhhDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarRrhh(Rrhh rrhh, String acc) {
        log.info("Registrando RR.HH.: {}, Accion: {}", rrhh.toString(), acc);
        return spHelper.ejecutarAbm(SP_ABM, rrhh, acc);
    }

    // ── L: el padrón ──────────────────────────────────────────────────────
    // @estado sólo se manda si viene con contenido: el SP filtra con
    // (@estado IS NULL OR ...), así que mandarlo vacío perdería las filas
    // dadas de baja, que son justamente las que el ABM tiene que mostrar
    // para poder reactivarlas.
    @Override
    public List<Rrhh> obtenerRrhh(String estado) {
        Map<String, Object> filtro = new HashMap<>();
        if (estado != null && !estado.trim().isEmpty()) {
            filtro.put("estado", estado);
        }
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", Rrhh.class);
    }
}
