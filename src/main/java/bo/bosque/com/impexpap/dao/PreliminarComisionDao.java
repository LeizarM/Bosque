package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.NotaPreliminar;
import bo.bosque.com.impexpap.model.PreliminarComision;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PreliminarComisionDao implements IPreliminarComision {

    /** SP heredado de Bosque v2. No se reescribe todavia: primero paridad. */
    private static final String SP_LIST = "p_list_paraPagar";

    private final SpHelper spHelper;

    public PreliminarComisionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<PreliminarComision> preliminarInterno(int mes, int anio, BigDecimal tc) {
        return ejecutar(mes, anio, tc, "F");
    }

    @Override
    public List<PreliminarComision> preliminarExterno(int mes, int anio, BigDecimal tc) {
        return ejecutar(mes, anio, tc, "I");
    }

    @Override
    public List<PreliminarComision> preliminarDinamicaAnterior(int mes, int anio, BigDecimal tc) {
        return ejecutar(mes, anio, tc, "J");
    }

    @Override
    public List<PreliminarComision> preliminarDinamicaVigente(int mes, int anio, BigDecimal tc) {
        return ejecutar(mes, anio, tc, "K");
    }


    @Override
    public List<NotaPreliminar> notasDeFila(int idVendedor, int mes, int anio, String comisionCad) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idVendedor", idVendedor);
        p.put("mes", mes);
        p.put("anio", anio);
        p.put("comisionCad", comisionCad);
        return spHelper.ejecutarListado(SP_LIST, p, "G1", NotaPreliminar.class);
    }

    /**
     * Se usa el overload de Map y no el de modelo porque el SP tiene mas de
     * treinta parametros y solo hay que mandar estos cuatro; el resto queda en
     * su valor por defecto dentro del SP.
     */
    private List<PreliminarComision> ejecutar(int mes, int anio, BigDecimal tc, String accion) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("mes", mes);
        p.put("anio", anio);
        if (tc != null) p.put("tc", tc);
        return spHelper.ejecutarListado(SP_LIST, p, accion, PreliminarComision.class);
    }
}
