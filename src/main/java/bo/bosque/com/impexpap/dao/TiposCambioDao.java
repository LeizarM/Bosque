package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TiposCambio;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public List<TiposCambio> obtenerTcVigenteRef(Long codBanco, long idMonedaOrigen, long idMonedaDestino) {
        // Acción 'V': TOP 1 último TC (fechaVigencia <= hoy) para banco+monedas.
        // Se usa el overload de Map para enviar SOLO los filtros que usa la acción;
        // el overload de modelo mandaría idTipoCambio/tasaCompra/etc. en 0 y, sobre
        // todo, codBanco=0 NO es lo mismo que NULL (BCB). codBanco null/0 → no se
        // envía → el SP lo trata como BCB (ISNULL(@codBanco,0)=ISNULL(codBanco,0)).
        Map<String, Object> filtro = new HashMap<>();
        if (codBanco != null && codBanco != 0L) {
            filtro.put("codBanco", codBanco);
        }
        filtro.put("idMonedaOrigen", idMonedaOrigen);
        filtro.put("idMonedaDestino", idMonedaDestino);
        return spHelper.ejecutarListado("p_list_tpex_TiposCambio", filtro, "V", TiposCambio.class);
    }
}

