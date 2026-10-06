package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.DetalleReciboRptDto;
import bo.bosque.com.impexpap.model.CbrDetalle;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a <b>tcbr_cbrDetalle</b> via {@link SpHelper}.
 *
 * <p>Mismo criterio que {@link GarantiaCbrDao}: cada llamada arma su Map con los parametros
 * exactos y omite los null (quedan en su DEFAULT NULL). En particular, el alta NO manda
 * {@code @fecha}: {@code p_abm_CbrDetalle} graba GETDATE() e ignora el valor.
 */
@Repository
public class CbrDetalleDao implements ICbrDetalle {

    private static final String SP_ABM = "p_abm_CbrDetalle";
    private static final String SP_LIST = "p_list_CbrDetalle";

    private final SpHelper spHelper;

    public CbrDetalleDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<CbrDetalle> listarPorGarantia(long codGarantia) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codGarantia", codGarantia);
        return spHelper.ejecutarListado(SP_LIST, p, "L", CbrDetalle.class);
    }

    @Override
    public CbrDetalle obtener(long codDetalle) {
        if (codDetalle <= 0) return null;
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codDetalle", codDetalle);
        List<CbrDetalle> filas = spHelper.ejecutarListado(SP_LIST, p, "L", CbrDetalle.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public List<DetalleReciboRptDto> listarParaRecibo(long codGarantia) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codGarantia", codGarantia);
        return spHelper.ejecutarListado(SP_LIST, p, "A", DetalleReciboRptDto.class);
    }

    @Override
    public RespuestaSp registrar(CbrDetalle d, String accion) {
        Map<String, Object> p = new LinkedHashMap<>();
        if ("U".equals(accion)) {
            p.put("codDetalle", d.getCodDetalle());
        } else {
            poner(p, "codGarantia", d.getCodGarantia());
            poner(p, "tipoGarantia", texto(d.getTipoGarantia()));
            poner(p, "detalle", texto(d.getDetalle()));
        }
        poner(p, "montoGarantiaParc", d.getMontoGarantiaParc());
        poner(p, "audUsuario", d.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM, p, accion);
    }

    @Override
    public RespuestaSp eliminar(long codDetalle, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codDetalle", codDetalle);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
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
