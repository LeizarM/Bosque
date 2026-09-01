package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioHrs;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BioHrsDao implements IBioHrs {

    private static final String SP_ABM  = "p_abm_BioHrs";
    private static final String SP_LIST = "p_list_BioHrs";

    private final SpHelper spHelper;

    public BioHrsDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(BioHrs item, String acc, Long audUsuario, String motivo) {
        log.info("Registrando BioHrs: {}, Accion: {}", item, acc);
        Map<String, Object> params = new HashMap<>();
        params.put("idHrs", item.getIdHrs());
        params.put("nombre", item.getNombre());
        params.put("ingreso", item.getIngreso());
        params.put("salida", item.getSalida());
        params.put("cantDias", item.getCantDias());
        params.put("cantMinutos", item.getCantMinutos());
        params.put("estado", item.getEstado());
        if (audUsuario != null) params.put("audUsuario", audUsuario);
        if (motivo != null && !motivo.trim().isEmpty()) params.put("motivo", motivo);
        return spHelper.ejecutarAbmMap(SP_ABM, params, acc);
    }

    @Override
    public List<BioHrs> listar(Map<String, Object> filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", BioHrs.class);
    }
}
