package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CargoPago;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CargoPagoDao implements ICargoPago {

    private final SpHelper spHelper;

    public CargoPagoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarCargoPago(CargoPago mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tpex_CargoPago", mb, acc);
    }

    @Override
    public List<CargoPago> obtenerCargoPago(long idCargo) {
        CargoPago filtro = new CargoPago();
        filtro.setIdCargo(idCargo);
        return spHelper.ejecutarListado("p_list_tpex_Cargos", filtro, "L", CargoPago.class);
    }

    @Override
    public List<CargoPago> obtenerCargoPorCotizacion(long idCotizacion) {
        CargoPago filtro = new CargoPago();
        filtro.setIdCotizacion(idCotizacion);
        return spHelper.ejecutarListado("p_list_tpex_Cargos", filtro, "C", CargoPago.class);
    }

    @Override
    public List<CargoPago> obtenerCargoPorTransaccion(long idTransaccion) {
        CargoPago filtro = new CargoPago();
        filtro.setIdTransaccion(idTransaccion);
        return spHelper.ejecutarListado("p_list_tpex_Cargos", filtro, "T", CargoPago.class);
    }
}

