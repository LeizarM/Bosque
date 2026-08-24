package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ComisionDinamica;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ComisionDinamicaDao implements IComisionDinamica {

    private static final String SP_ABM  = "p_abm_tcom_ComisionDinamica";
    private static final String SP_LIST = "p_list_tcom_ComisionDinamica";

    private final SpHelper spHelper;

    public ComisionDinamicaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarComisionDinamica(ComisionDinamica mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    @Override
    public List<ComisionDinamica> obtenerPorId(long idDc) {
        // Map y no modelo: ejecutarListado(model, ...) enviaria tambien metaUsd,
        // porcentaje y demas, que no son parametros de este SP.
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idDC", idDc);
        p.put("esInterno", null);
        return spHelper.ejecutarListado(SP_LIST, p, "L", ComisionDinamica.class);
    }

    @Override
    public List<ComisionDinamica> obtenerVigentes(Integer esInterno, Date fecha) {
        // Map y no model: esInterno null significa "sin filtro" y el overload de
        // model manda 0, que el SP leeria como "solo externas".
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idDc", 0);
        p.put("esInterno", esInterno);
        p.put("fecha", fecha);
        return spHelper.ejecutarListado(SP_LIST, p, "V", ComisionDinamica.class);
    }

    @Override
    public List<ComisionDinamica> obtenerTodas(Integer esInterno) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idDc", 0);
        p.put("esInterno", esInterno);
        return spHelper.ejecutarListado(SP_LIST, p, "E", ComisionDinamica.class);
    }
}
