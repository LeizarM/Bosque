package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TipoRecibo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TipoReciboDao implements ITipoRecibo {

    private final SpHelper spHelper;

    public TipoReciboDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarTipoRecibo(TipoRecibo mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tmto_TipoRecibo", mb, acc);
    }

    /*
     * Los listados van con el overload de Map, no con el de modelo:
     * p_list_tmto_* usa NULL como "sin filtro", y el overload de modelo
     * CONSERVA los 0 de los primitivos, con lo que filtraria por id = 0
     * y devolveria vacio.
     */
    @Override
    public List<TipoRecibo> obtenerTipoRecibo(long codTipoRecibo) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("codTipoRecibo", codTipoRecibo);
        return spHelper.ejecutarListado("p_list_tmto_TipoRecibo", filtro, "L", TipoRecibo.class);
    }

    @Override
    public List<TipoRecibo> listarTipoRecibo() {
        return spHelper.ejecutarListado("p_list_tmto_TipoRecibo",
                new HashMap<String, Object>(), "A", TipoRecibo.class);
    }
}
