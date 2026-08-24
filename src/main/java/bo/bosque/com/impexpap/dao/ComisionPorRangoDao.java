package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ComisionPorRango;
import bo.bosque.com.impexpap.model.TipoCambioComision;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ComisionPorRangoDao implements IComisionPorRango {

    private static final String SP_ABM  = "p_abm_tcom_ComisionPorRango";
    private static final String SP_LIST = "p_list_tcom_ComisionPorRango";
    private static final String SP_TC   = "p_list_tcom_TipoCambio";

    private final SpHelper spHelper;

    public ComisionPorRangoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarRango(ComisionPorRango mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    @Override
    public List<ComisionPorRango> obtenerRangos() {
        return spHelper.ejecutarListado(SP_LIST, filtro(0L, null), "L", ComisionPorRango.class);
    }

    @Override
    public List<ComisionPorRango> obtenerPorTipo(String tipo) {
        return spHelper.ejecutarListado(SP_LIST, filtro(0L, tipo), "T", ComisionPorRango.class);
    }

    @Override
    public List<TipoCambioComision> obtenerTipoCambio(java.util.Date fecha) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("fecha", fecha);
        return spHelper.ejecutarListado(SP_TC, p, "V", TipoCambioComision.class);
    }

    /** Solo los parametros que declara el SP: el overload de modelo enviaria de mas. */
    private Map<String, Object> filtro(long idCFR, String tipo) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idCFR", idCFR);
        p.put("tipo", tipo);
        return p;
    }
}
