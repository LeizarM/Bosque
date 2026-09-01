package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BioEmplBosqEmpl;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BioEmplBosqEmplDao implements IBioEmplBosqEmpl {

    private static final String SP_ABM  = "p_abm_BioEmplBosqEmpl";
    private static final String SP_LIST = "p_list_BioEmplBosqEmpl";

    private final SpHelper spHelper;

    public BioEmplBosqEmplDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(BioEmplBosqEmpl item, String acc, Long audUsuario) {
        log.info("Registrando BioEmplBosqEmpl: {}, Accion: {}", item, acc);
        Map<String, Object> params = new HashMap<>();
        params.put("idEmpleadBio", item.getIdEmpleadBio());
        params.put("datoNombreBiom", item.getDatoNombreBiom());
        params.put("idEmpleado", item.getIdEmpleado());
        params.put("datoNombreBosq", item.getDatoNombreBosq());
        if (audUsuario != null) params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, params, acc);
    }

    @Override
    public List<BioEmplBosqEmpl> listar(Map<String, Object> filtro) {
        return spHelper.ejecutarListado(SP_LIST, filtro, "L", BioEmplBosqEmpl.class);
    }
}
