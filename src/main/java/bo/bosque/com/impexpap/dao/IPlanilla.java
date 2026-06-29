package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Planilla;
import bo.bosque.com.impexpap.model.PlanillaDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

public interface IPlanilla {
    List<Planilla> listarPlanilla(Planilla p);
    List<PlanillaDetalle> listarPlanillaDetalle(PlanillaDetalle pd);
    RespuestaSp abmPlanilla(Planilla p, String acc);
    List<Map<String, Object>> listarPagosBanco(int mes, int anio, int codBanco, Integer codEmpresa);
}
