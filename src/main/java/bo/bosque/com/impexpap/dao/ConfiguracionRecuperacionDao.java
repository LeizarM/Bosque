package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.ConfiguracionRecuperacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parámetros de la recuperación de acceso (trec_configuracion). */
@Repository
public class ConfiguracionRecuperacionDao implements IConfiguracionRecuperacion {

    private static final String SP_ABM = "p_abm_trec_Configuracion";
    private static final String SP_LIST = "p_list_trec_Configuracion";

    private final SpHelper spHelper;

    public ConfiguracionRecuperacionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<ConfiguracionRecuperacion> listar() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "L", ConfiguracionRecuperacion.class);
    }

    @Override
    public ConfiguracionRecuperacion obtener(String clave) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("clave", clave);
        List<ConfiguracionRecuperacion> filas = spHelper.ejecutarListado(SP_LIST, p, "L", ConfiguracionRecuperacion.class);
        return filas == null || filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public RespuestaSp actualizar(String clave, String valor, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("clave", clave);
        p.put("valor", valor);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "U");
    }
}
