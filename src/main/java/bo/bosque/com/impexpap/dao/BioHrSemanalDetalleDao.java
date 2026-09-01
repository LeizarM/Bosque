package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioHrSemanalDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BioHrSemanalDetalleDao implements IBioHrSemanalDetalle {

    private static final String SP_ABM  = "p_abm_BioHrSemanalDetalle";
    private static final String SP_LIST = "p_list_BioHrSemanalDetalle";

    private final SpHelper spHelper;

    public BioHrSemanalDetalleDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(BioHrSemanalDetalle item, String acc, Long audUsuario, String motivo) {
        log.info("Registrando BioHrSemanalDetalle: {}, Accion: {}", item, acc);
        Map<String, Object> params = new HashMap<>();
        params.put("idHrDet", item.getIdHrDet());
        params.put("idHrSemanal", item.getIdHrSemanal());
        params.put("idHrs", item.getIdHrs());
        params.put("dia", item.getDia());
        if (audUsuario != null) params.put("audUsuario", audUsuario);
        if (motivo != null && !motivo.trim().isEmpty()) params.put("motivo", motivo);
        return spHelper.ejecutarAbmMap(SP_ABM, params, acc);
    }

    @Override
    public List<BioHrSemanalDetalle> listar(Map<String, Object> filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", BioHrSemanalDetalle.class);
    }
}
