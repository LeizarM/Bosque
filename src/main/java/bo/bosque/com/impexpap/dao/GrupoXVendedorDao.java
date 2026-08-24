package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.GrupoXVendedor;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class GrupoXVendedorDao implements IGrupoXVendedor {

    private static final String SP_ABM  = "p_abm_tcom_GrupoXVendedor";
    private static final String SP_LIST = "p_list_tcom_GrupoXVendedor";

    private final SpHelper spHelper;

    public GrupoXVendedorDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarGrupoXVendedor(GrupoXVendedor mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    @Override
    public List<GrupoXVendedor> obtenerPorId(long idGrpVen) {
        return spHelper.ejecutarListado(SP_LIST, filtro(idGrpVen, 0L), "L", GrupoXVendedor.class);
    }

    @Override
    public List<GrupoXVendedor> obtenerPorVendedor(long idVendedor) {
        return spHelper.ejecutarListado(SP_LIST, filtro(0L, idVendedor), "V", GrupoXVendedor.class);
    }

    @Override
    public List<GrupoXVendedor> obtenerVigentes() {
        return spHelper.ejecutarListado(SP_LIST, filtro(0L, 0L), "A", GrupoXVendedor.class);
    }

    /** Exactamente los parametros que declara p_list_tcom_GrupoXVendedor. */
    private Map<String, Object> filtro(long idGrpVen, long idVendedor) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idGrpVen", idGrpVen);
        p.put("idVendedor", idVendedor);
        return p;
    }
}
