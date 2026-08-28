package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TalonarioDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TalonarioDetalleDao implements ITalonarioDetalle {

    private final SpHelper spHelper;

    public TalonarioDetalleDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarTalonarioDetalle(TalonarioDetalle mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tmto_TalonarioDetalle", mb, acc);
    }

    @Override
    public List<TalonarioDetalle> obtenerTalonarioDetalle(long codDetalle) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codDetalle", codDetalle);
        return spHelper.ejecutarListado("p_list_tmto_TalonarioDetalle", filtro, "L", TalonarioDetalle.class);
    }

    @Override
    public List<TalonarioDetalle> listarPorTalonario(long codTalonario) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codTalonario", codTalonario);
        return spHelper.ejecutarListado("p_list_tmto_TalonarioDetalle", filtro, "T", TalonarioDetalle.class);
    }
}
