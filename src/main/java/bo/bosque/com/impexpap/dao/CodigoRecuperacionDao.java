package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CodigoRecuperacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Códigos de recuperación de acceso. Cada acción manda solo sus parámetros (Map), nunca el modelo entero. */
@Repository
public class CodigoRecuperacionDao implements ICodigoRecuperacion {

    private static final String SP_ABM = "p_abm_trec_CodigoRecuperacion";
    private static final String SP_LIST = "p_list_trec_CodigoRecuperacion";

    private final SpHelper spHelper;

    public CodigoRecuperacionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp solicitar(String login, String codigoHash, String ip) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("login", login);
        p.put("codigoHash", codigoHash);
        p.put("ip", ip);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "I");
    }

    @Override
    public CodigoRecuperacion obtenerPorId(long idCodigo) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idCodigo", idCodigo);
        return primero(spHelper.ejecutarListado(SP_LIST, p, "L", CodigoRecuperacion.class));
    }

    @Override
    public CodigoRecuperacion obtenerVigente(String login) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("login", login);
        return primero(spHelper.ejecutarListado(SP_LIST, p, "V", CodigoRecuperacion.class));
    }

    @Override
    public RespuestaSp reservarIntento(long idCodigo) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idCodigo", idCodigo);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "F");
    }

    @Override
    public RespuestaSp restablecer(long idCodigo, String password2, String ip) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idCodigo", idCodigo);
        p.put("password2", password2);
        p.put("ip", ip);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "R");
    }

    @Override
    public RespuestaSp eliminar(long idCodigo) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idCodigo", idCodigo);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }

    private static CodigoRecuperacion primero(List<CodigoRecuperacion> filas) {
        return filas == null || filas.isEmpty() ? null : filas.get(0);
    }
}
