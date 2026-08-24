package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.EstadoPeriodo;
import bo.bosque.com.impexpap.model.SincronizacionNotas;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class EjecucionComisionDao implements IEjecucionComision {

    private static final String SP_ESTADO   = "p_list_tcom_EstadoPeriodo";
    private static final String SP_CARGAR   = "p_abm_tcom_CargarNotas";
    private static final String SP_EJECUTAR = "p_abm_tcom_EjecutarPago";
    private static final String SP_SINCRO   = "p_abm_tcom_SincronizarNotas";

    private final SpHelper spHelper;

    public EjecucionComisionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<EstadoPeriodo> obtenerEstadoPeriodo(int mes, int anio, int esInterno) {
        return spHelper.ejecutarListado(SP_ESTADO, periodo(mes, anio, esInterno, 0L),
                "L", EstadoPeriodo.class);
    }

    @Override
    public RespuestaSp cargarNotas(int mes, int anio, int esInterno, long audUsuario) {
        return spHelper.ejecutarAbmMap(SP_CARGAR, periodo(mes, anio, esInterno, audUsuario), "I");
    }

    @Override
    public RespuestaSp ejecutarPago(int mes, int anio, int esInterno, long audUsuario) {
        return spHelper.ejecutarAbmMap(SP_EJECUTAR, periodo(mes, anio, esInterno, audUsuario), "I");
    }

    @Override
    public List<SincronizacionNotas> sincronizarNotas(long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarListado(SP_SINCRO, p, "S", SincronizacionNotas.class);
    }

    /** LinkedHashMap para que el orden de los parametros sea estable. */
    private Map<String, Object> periodo(int mes, int anio, int esInterno, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("mes", mes);
        p.put("anio", anio);
        p.put("esInterno", esInterno);
        if (audUsuario > 0) p.put("audUsuario", audUsuario);
        return p;
    }
}
