package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioHrXEmplExpandido;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BioHrXEmplExpandidoDao implements IBioHrXEmplExpandido {

    private static final String SP_ABM  = "p_abm_BioHrXEmplExpandido";
    private static final String SP_LIST = "p_list_BioHrXEmplExpandido";

    private final SpHelper spHelper;

    public BioHrXEmplExpandidoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(BioHrXEmplExpandido item, String acc, Long audUsuario) {
        log.info("Registrando BioHrXEmplExpandido: {}, Accion: {}", item, acc);
        Map<String, Object> params = new HashMap<>();
        params.put("idHrEmpleado", item.getIdHrEmpleado());
        params.put("idHrs", item.getIdHrs());
        params.put("jornada", item.getJornada());
        params.put("dia", item.getDia());
        params.put("hrIngreso", item.getHrIngreso());
        params.put("hrSalida", item.getHrSalida());
        if (audUsuario != null) params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, acc);
    }

    @Override
    public List<BioHrXEmplExpandido> listar(Map<String, Object> filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", BioHrXEmplExpandido.class);
    }

    @Override
    public RespuestaSp borrarMes(long idHrEmpleado, Date unDiaDelMes, Long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idHrEmpleado", idHrEmpleado);
        params.put("jornada", unDiaDelMes);
        if (audUsuario != null) params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "D");
    }

    @Override
    public RespuestaSp generarMes(long idEmpleado, Date unDiaDelMes, Long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idEmpleado", idEmpleado);
        params.put("audFecha", unDiaDelMes);
        if (audUsuario != null) params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "A");
    }
}
