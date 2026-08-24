package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Vendedor;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class VendedorDao implements IVendedor {

    private static final String SP_ABM  = "p_abm_tcom_Vendedor";
    private static final String SP_LIST = "p_list_tcom_Vendedor";

    private final SpHelper spHelper;

    public VendedorDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarVendedor(Vendedor mb, String acc) {
        return spHelper.ejecutarAbm(SP_ABM, mb, acc);
    }

    @Override
    public List<Vendedor> obtenerVendedores(long idVendedor) {
        return spHelper.ejecutarListado(SP_LIST, filtro(idVendedor, 0), "L", Vendedor.class);
    }

    @Override
    public List<Vendedor> obtenerVendedoresPorEmpresa(int bd) {
        return spHelper.ejecutarListado(SP_LIST, filtro(0L, bd), "B", Vendedor.class);
    }

    @Override
    public List<Vendedor> obtenerVendedoresTodos() {
        return spHelper.ejecutarListado(SP_LIST, filtro(0L, 0), "E", Vendedor.class);
    }

    /** Exactamente los parametros que declara p_list_tcom_Vendedor. */
    private Map<String, Object> filtro(long idVendedor, int bd) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idVendedor", idVendedor);
        p.put("bd", bd);
        return p;
    }
}
