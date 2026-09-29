package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.model.CiudadVenta;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

@Repository
public class UsuarioCiudadDao implements IUsuarioCiudad {

    private static final String SP_ABM = "p_abm_tven_UsuarioCiudad";
    private static final String SP_LIST = "p_list_tven_UsuarioCiudad";

    private final SpHelper spHelper;

    public UsuarioCiudadDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<CiudadVenta> ciudadesVenta() {
        return spHelper.ejecutarListado(SP_LIST, new LinkedHashMap<>(), "V", CiudadVenta.class);
    }

    @Override
    public List<CiudadVenta> asignadasA(long codUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codUsuario", codUsuario);
        return spHelper.ejecutarListado(SP_LIST, p, "U", CiudadVenta.class);
    }

    @Override
    public List<CiudadVenta> todasLasAsignaciones() {
        return spHelper.ejecutarListado(SP_LIST, new LinkedHashMap<>(), "A", CiudadVenta.class);
    }

    @Override
    public RespuestaSp asignar(long codUsuario, int codCiudad, long audUsuario) {
        return spHelper.ejecutarAbmMap(SP_ABM, parametros(codUsuario, codCiudad, audUsuario), "I");
    }

    @Override
    public RespuestaSp quitar(long codUsuario, int codCiudad, long audUsuario) {
        return spHelper.ejecutarAbmMap(SP_ABM, parametros(codUsuario, codCiudad, audUsuario), "D");
    }

    private static Map<String, Object> parametros(long codUsuario, int codCiudad, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codUsuario", codUsuario);
        p.put("codCiudad", codCiudad);
        p.put("audUsuario", audUsuario);
        return p;
    }
}
