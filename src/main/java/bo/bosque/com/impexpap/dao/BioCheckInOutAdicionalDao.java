package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioCheckInOutAdicional;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BioCheckInOutAdicionalDao implements IBioCheckInOutAdicional {

    private static final String SP_ABM  = "p_abm_BioCHECKINOUTAdicinal";
    private static final String SP_LIST = "p_list_BioCHECKINOUTAdicinal";

    private final SpHelper spHelper;

    public BioCheckInOutAdicionalDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(BioCheckInOutAdicional item, String acc, Long audUsuario, String motivo) {
        log.info("Registrando BioCheckInOutAdicional: {}, Accion: {}", item, acc);
        Map<String, Object> params = new HashMap<>();
        params.put("USERID", item.getUSERID());
        params.put("CHECKTIME", item.getCHECKTIME());
        params.put("CODEMPLEADO", item.getCODEMPLEADO());
        params.put("fechaString", item.getFechaString());
        if (audUsuario != null) params.put("audUsuario", audUsuario);
        if (motivo != null && !motivo.trim().isEmpty()) params.put("motivo", motivo);
        return spHelper.ejecutarAbmMap(SP_ABM, params, acc);
    }

    @Override
    public List<BioCheckInOutAdicional> listar(Map<String, Object> filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", BioCheckInOutAdicional.class);
    }
}
