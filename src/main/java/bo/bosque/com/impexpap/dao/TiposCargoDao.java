package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TiposCargo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TiposCargoDao implements ITiposCargo {

    private final SpHelper spHelper;

    public TiposCargoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarTiposCargo(TiposCargo mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tpex_TiposCargo", mb, acc);
    }

    @Override
    public List<TiposCargo> obtenerTiposCargo(long idTipoCargo) {
        TiposCargo filtro = new TiposCargo();
        filtro.setIdTipoCargo(idTipoCargo);
        return spHelper.ejecutarListado("p_list_tpex_TiposCargo", filtro, "L", TiposCargo.class);
    }
}

