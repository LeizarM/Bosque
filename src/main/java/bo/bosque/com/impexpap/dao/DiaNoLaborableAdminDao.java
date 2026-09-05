package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DiaNoLaborableAdmin;
import bo.bosque.com.impexpap.model.DiaNoLaborableAdminSucursal;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DiaNoLaborableAdminDao implements IDiaNoLaborableAdmin {

    private final SpHelper spHelper;

    public DiaNoLaborableAdminDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarDiaNoLaborable(DiaNoLaborableAdmin mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_rrhh_DiaNoLaborable", mb, acc);
    }

    @Override
    public List<DiaNoLaborableAdmin> obtenerDiasNoLaborables(long idDiaNoLaborable, int gestion) {
        // Map overload: idDiaNoLaborable/gestion en 0 se OMITEN para que el SP reciba
        // NULL (su condicion de "sin filtro"), en vez de filtrar por el valor literal 0.
        Map<String, Object> filtro = new HashMap<>();
        if (idDiaNoLaborable > 0) filtro.put("idDiaNoLaborable", idDiaNoLaborable);
        if (gestion > 0) filtro.put("gestion", gestion);
        return spHelper.ejecutarListado("p_list_rrhh_DiaNoLaborable", filtro, "L", DiaNoLaborableAdmin.class);
    }

    @Override
    public List<DiaNoLaborableAdminSucursal> obtenerSucursales(long idDiaNoLaborable) {
        Map<String, Object> filtro = new HashMap<>();
        if (idDiaNoLaborable > 0) filtro.put("idDiaNoLaborable", idDiaNoLaborable);
        return spHelper.ejecutarListado("p_list_rrhh_DiaNoLaborable_sucursal", filtro, "S", DiaNoLaborableAdminSucursal.class);
    }
}
