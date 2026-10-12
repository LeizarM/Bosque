package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Indemnizacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
@Repository
public class IndemnizacionDao implements IIndemnizacion {

    private final SpHelper spHelper;

    public IndemnizacionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<Indemnizacion> listarIndemnizacion(Indemnizacion indemnizacion) {
        return this.spHelper.ejecutarListado("p_list_Indemnizacion", indemnizacion, "C", Indemnizacion.class);
    }

    @Override
    public List<Indemnizacion> listarHistorialIndemnizacion(Indemnizacion indemnizacion) {
        return this.spHelper.ejecutarListado("p_list_Indemnizacion", indemnizacion, "L", Indemnizacion.class);
    }

    @Override
    public RespuestaSp abmIndemnizacion(Indemnizacion indemnizacion, String acc) {
        return this.spHelper.ejecutarAbm("p_abm_Indemnizacion", indemnizacion, acc);
    }

    @Override
    public List<Map<String, Object>> reporteProvision(String fechaCorte) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("fechaCorte", fechaCorte);
        params.put("tipoReporte", "TODOS");
        params.put("ACCION", "R");
        return this.spHelper.ejecutarListadoDinamico("p_list_Indemnizacion", params);
    }
}
