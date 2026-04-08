package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TiposCambio;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TiposCambioDao implements ITiposCambio {

    private final SpHelper spHelper;

    public TiposCambioDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarTiposCambio(TiposCambio mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tpex_TiposCambio", mb, acc);
    }

    @Override
    public List<TiposCambio> obtenerTiposCambio(long idTipoCambio) {
        TiposCambio filtro = new TiposCambio();
        filtro.setIdTipoCambio(idTipoCambio);
        return spHelper.ejecutarListado("p_list_tpex_TiposCambio", filtro, "L", TiposCambio.class);
    }

    @Override
    public List<TiposCambio> obtenerTiposCambioPorBanco(int codBanco) {
        TiposCambio filtro = new TiposCambio();
        filtro.setCodBanco(codBanco);
        return spHelper.ejecutarListado("p_list_tpex_TiposCambio", filtro, "R", TiposCambio.class);
    }
}

