package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Planilla;
import bo.bosque.com.impexpap.model.PlanillaDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PlanillaDao implements IPlanilla {

    private final SpHelper spHelper;

    public PlanillaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<Planilla> listarPlanilla(Planilla p) {
        return this.spHelper.ejecutarListado("p_list_Planilla", p, "C", Planilla.class);
    }

    @Override
    public List<PlanillaDetalle> listarPlanillaDetalle(PlanillaDetalle pd) {
        return this.spHelper.ejecutarListado("p_list_PlanillaDetalle", pd, "D", PlanillaDetalle.class);
    }

    @Override
    public RespuestaSp abmPlanilla(Planilla p, String acc) {
        return this.spHelper.ejecutarAbm("p_abm_Planilla", p, acc);
    }

    @Override
    public List<Map<String, Object>> listarPagosBanco(int mes, int anio, int codBanco, Integer codEmpresa) {
        Map<String, Object> params = new HashMap<>();
        params.put("mes", mes);
        params.put("anio", anio);
        params.put("codBanco", codBanco);
        params.put("codEmpresa", codEmpresa);
        
        return this.spHelper.ejecutarListadoDinamico("p_list_PlanillaPagosBanco", params);
    }
}
