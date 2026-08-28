package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TalonarioPorGrupo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TalonarioPorGrupoDao implements ITalonarioPorGrupo {

    private final SpHelper spHelper;

    public TalonarioPorGrupoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarTalonarioPorGrupo(TalonarioPorGrupo mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tmto_TalonarioPorGrupo", mb, acc);
    }

    @Override
    public List<TalonarioPorGrupo> listarPorGrupo(Long codGrupo) {
        Map<String, Object> filtro = new HashMap<>();
        // null = todas las asignaciones; el SP deja el parametro en DEFAULT NULL
        if (codGrupo != null) filtro.put("codGrupo", codGrupo);
        return spHelper.ejecutarListado("p_list_tmto_TalonarioPorGrupo", filtro, "L", TalonarioPorGrupo.class);
    }

    @Override
    public List<TalonarioPorGrupo> listarTiposDisponibles(long codGrupo) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codGrupo", codGrupo);
        return spHelper.ejecutarListado("p_list_tmto_TalonarioPorGrupo", filtro, "S", TalonarioPorGrupo.class);
    }
}
