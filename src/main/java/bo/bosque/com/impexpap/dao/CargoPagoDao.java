package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CargoPago;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CargoPagoDao implements ICargoPago {

    private final SpHelper spHelper;

    public CargoPagoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarCargoPago(CargoPago mb, String acc) {
        // FIX: el SP real es p_abm_tpex_Cargos (no existe p_abm_tpex_CargoPago).
        return spHelper.ejecutarAbm("p_abm_tpex_Cargos", mb, acc);
    }

    @Override
    public List<CargoPago> obtenerCargoPago(long idCargo) {
        CargoPago filtro = new CargoPago();
        filtro.setIdCargo(idCargo);
        return spHelper.ejecutarListado("p_list_tpex_Cargos", filtro, "L", CargoPago.class);
    }

    // ── Cargos de una cotización ───────────────────────────────────────────
    // FIX: el SP solo implementa las acciones L y R (no C/T). La acción L ya
    //      filtra por idCotizacion/idTransaccion. Se usa Map para enviar SOLO
    //      @idCotizacion; con el overload de modelo, idTransaccion=0 (default
    //      de long) se enviaría como filtro real y no devolvería filas.
    @Override
    public List<CargoPago> obtenerCargoPorCotizacion(long idCotizacion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCotizacion", idCotizacion);
        return spHelper.ejecutarListado("p_list_tpex_Cargos", filtro, "L", CargoPago.class);
    }

    // ── Cargos de una transacción ──────────────────────────────────────────
    // FIX: idem; acción L + Map con SOLO @idTransaccion.
    @Override
    public List<CargoPago> obtenerCargoPorTransaccion(long idTransaccion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTransaccion", idTransaccion);
        return spHelper.ejecutarListado("p_list_tpex_Cargos", filtro, "L", CargoPago.class);
    }
}

