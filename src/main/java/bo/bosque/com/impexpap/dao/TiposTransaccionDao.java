package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TiposTransaccion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TiposTransaccionDao implements ITiposTransaccion {

    private final SpHelper spHelper;

    public TiposTransaccionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarTiposTransaccion(TiposTransaccion mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tpex_TiposTransaccion", mb, acc);
    }

    @Override
    public List<TiposTransaccion> obtenerTiposTransaccion(long idTipoTransaccion) {
        TiposTransaccion filtro = new TiposTransaccion();
        filtro.setIdTipoTransaccion(idTipoTransaccion);
        return spHelper.ejecutarListado("p_list_tpex_TiposTransaccion", filtro, "L", TiposTransaccion.class);
    }
}

