package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.model.AccionCbr;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a <b>tcbr_accion</b> via {@link SpHelper}. Mismo criterio de Maps que
 * {@link GarantiaCbrDao}.
 *
 * <p>Las reglas de negocio (REG y TRASP no se cargan a mano; todo lo que no sea NOT exige
 * traspaso) viven en {@code p_abm_AccionCbr}: este DAO no las repite.
 */
@Repository
public class AccionCbrDao implements IAccionCbr {

    private static final String SP_ABM = "p_abm_AccionCbr";
    private static final String SP_LIST = "p_list_AccionCbr";

    private final SpHelper spHelper;

    public AccionCbrDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<AccionCbr> listarPorGarantia(long codGarantia) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codGarantia", codGarantia);
        return spHelper.ejecutarListado(SP_LIST, p, "L", AccionCbr.class);
    }

    @Override
    public AccionCbr obtener(long codAccion) {
        if (codAccion <= 0) return null;
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codAccion", codAccion);
        List<AccionCbr> filas = spHelper.ejecutarListado(SP_LIST, p, "L", AccionCbr.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * La rama 'B' devuelve una sola columna sin modelo ({@code totalRegistros}), por eso va
     * por {@code ejecutarListadoDinamico}, que exige mandar {@code ACCION} dentro del Map.
     */
    @Override
    public long traspasosPendientes() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("ACCION", "B");
        List<Map<String, Object>> filas = spHelper.ejecutarListadoDinamico(SP_LIST, p);
        if (filas.isEmpty()) return 0L;
        Object total = filas.get(0).get("totalRegistros");
        return total instanceof Number ? ((Number) total).longValue() : 0L;
    }

    @Override
    public RespuestaSp registrar(AccionCbr a, String accionAbm) {
        Map<String, Object> p = new LinkedHashMap<>();
        if ("U".equals(accionAbm)) {
            p.put("codAccion", a.getCodAccion());
        } else {
            poner(p, "codGarantia", a.getCodGarantia());
            poner(p, "fecha", a.getFecha());
            poner(p, "estado", texto(a.getEstado()));
        }
        poner(p, "observacion", texto(a.getObservacion()));
        poner(p, "audUsuario", a.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, accionAbm);
    }

    @Override
    public RespuestaSp eliminar(long codAccion, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codAccion", codAccion);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }

    @Override
    public RespuestaSp traspasar(int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "T");
    }

    private static void poner(Map<String, Object> p, String nombre, Object valor) {
        if (valor != null) p.put(nombre, valor);
    }

    private static String texto(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
