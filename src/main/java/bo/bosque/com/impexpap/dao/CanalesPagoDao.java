package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CanalesPago;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CanalesPagoDao implements ICanalesPago {

    private final SpHelper spHelper;

    public CanalesPagoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarCanalesPago(CanalesPago mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tpex_CanalesPago", mb, acc);
    }

    @Override
    public List<CanalesPago> obtenerCanalesPago(long idCanal) {
        CanalesPago filtro = new CanalesPago();
        filtro.setIdCanal(idCanal);
        return spHelper.ejecutarListado("p_list_tpex_CanalesPago", filtro, "L", CanalesPago.class);
    }
}

