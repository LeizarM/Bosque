package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioHrEmpleado;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BioHrEmpleadoDao implements IBioHrEmpleado {

    private static final String SP_ABM  = "p_abm_BioHrEmpleado";
    private static final String SP_LIST = "p_list_BioHrEmpleado";

    private final SpHelper spHelper;

    public BioHrEmpleadoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(BioHrEmpleado item, String acc, Long audUsuario, String motivo) {
        log.info("Registrando BioHrEmpleado: {}, Accion: {}", item, acc);
        Map<String, Object> params = new HashMap<>();
        params.put("idHrEmpleado", item.getIdHrEmpleado());
        params.put("idHrSemanal", item.getIdHrSemanal());
        params.put("idEmplead", item.getIdEmplead());
        params.put("inicio", item.getInicio());
        if (audUsuario != null) params.put("audUsuario", audUsuario);
        if (motivo != null && !motivo.trim().isEmpty()) params.put("motivo", motivo);
        return spHelper.ejecutarAbmMap(SP_ABM, params, acc);
    }

    @Override
    public List<BioHrEmpleado> listar(Map<String, Object> filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", BioHrEmpleado.class);
    }
}
