package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioHrSemanal;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BioHrSemanalDao implements IBioHrSemanal {

    private static final String SP_ABM  = "p_abm_BioHrSemanal";
    private static final String SP_LIST = "p_list_BioHrSemanal";

    private final SpHelper spHelper;

    public BioHrSemanalDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(BioHrSemanal item, String acc, Long audUsuario, String motivo) {
        log.info("Registrando BioHrSemanal: {}, Accion: {}", item, acc);
        Map<String, Object> params = new HashMap<>();
        params.put("idHrSemanal", item.getIdHrSemanal());
        params.put("nombre", item.getNombre());
        params.put("estado", item.getEstado());
        if (audUsuario != null) params.put("audUsuario", audUsuario);
        if (motivo != null && !motivo.trim().isEmpty()) params.put("motivo", motivo);
        return spHelper.ejecutarAbmMap(SP_ABM, params, acc);
    }

    @Override
    public List<BioHrSemanal> listar(Map<String, Object> filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", BioHrSemanal.class);
    }
}
