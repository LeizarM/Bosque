package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Sesion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sesiones de la app. Cada acción manda solo sus parámetros (Map), nunca el modelo entero;
 * los null no se mandan y el SP usa su DEFAULT.
 */
@Repository
public class SesionDao implements ISesion {

    private static final String SP_ABM = "p_abm_trec_Sesion";
    private static final String SP_LIST = "p_list_trec_Sesion";

    private final SpHelper spHelper;

    public SesionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public long iniciar(long codUsuario, String plataforma, String versionApp, String ip, int minutosVigencia) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codUsuario", codUsuario);
        poner(p, "plataforma", plataforma);
        poner(p, "versionApp", versionApp);
        poner(p, "ip", ip);
        p.put("minutosVigencia", minutosVigencia);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I").getIdGenerado();
    }

    @Override
    public void senal(long idSesion, long codUsuario, String plataforma, String versionApp, String ip) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idSesion", idSesion);
        p.put("codUsuario", codUsuario);
        poner(p, "plataforma", plataforma);
        poner(p, "versionApp", versionApp);
        poner(p, "ip", ip);
        spHelper.ejecutarAbmMap(SP_ABM, p, "S");
    }

    @Override
    public RespuestaSp cerrar(long idSesion, long codUsuario, String motivo, Long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idSesion", idSesion);
        p.put("codUsuario", codUsuario);
        p.put("motivoCierre", motivo);
        poner(p, "audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "C");
    }

    @Override
    public long cerrarTodas(long codUsuario, String motivo, Long audUsuario, long exceptoIdSesion) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (exceptoIdSesion > 0) p.put("idSesion", exceptoIdSesion);
        p.put("codUsuario", codUsuario);
        p.put("motivoCierre", motivo);
        poner(p, "audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "T").getIdGenerado();
    }

    @Override
    public List<Sesion> abiertas() {
        return spHelper.ejecutarListado(SP_LIST, new LinkedHashMap<>(), "A", Sesion.class);
    }

    @Override
    public List<Sesion> historial(Date desde, Date hasta, Long codUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("desde", desde);
        p.put("hasta", hasta);
        poner(p, "codUsuario", codUsuario);
        return spHelper.ejecutarListado(SP_LIST, p, "H", Sesion.class);
    }

    private static void poner(Map<String, Object> p, String clave, Object valor) {
        if (valor != null) p.put(clave, valor);
    }
}
