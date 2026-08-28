package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TalonarioGrupo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TalonarioGrupoDao implements ITalonarioGrupo {

    private final SpHelper spHelper;

    public TalonarioGrupoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarTalonarioGrupo(TalonarioGrupo mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tmto_TalonarioGrupo", mb, acc);
    }

    @Override
    public List<TalonarioGrupo> obtenerTalonarioGrupo(long codGrupo) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codGrupo", codGrupo);
        return spHelper.ejecutarListado("p_list_tmto_TalonarioGrupo", filtro, "L", TalonarioGrupo.class);
    }

    @Override
    public List<TalonarioGrupo> listarTalonarioGrupo() {
        return spHelper.ejecutarListado("p_list_tmto_TalonarioGrupo",
                new HashMap<String, Object>(), "A", TalonarioGrupo.class);
    }
}
