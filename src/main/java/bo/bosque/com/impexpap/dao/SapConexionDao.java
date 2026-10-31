package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SapConexion;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Las conexiones a SAP que comparten los módulos ({@code tsap_Conexion}). */
@Repository
public class SapConexionDao implements ISapConexion {

    private static final String SP_ABM = "p_abm_tsap_Conexion";
    private static final String SP_LIST = "p_list_tsap_Conexion";

    private final SpHelper spHelper;

    public SapConexionDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<SapConexion> listar() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "L", SapConexion.class);
    }

    @Override
    public SapConexion obtener(int idConexion) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idConexion", idConexion);
        List<SapConexion> filas = spHelper.ejecutarListado(SP_LIST, p, "L", SapConexion.class);
        return filas == null || filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public RespuestaSp guardar(SapConexion c, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idConexion", c.getIdConexion());
        p.put("nombre", c.getNombre());
        p.put("codEmpresa", c.getCodEmpresa());
        p.put("ambiente", c.getAmbiente());
        p.put("linkedServer", c.getLinkedServer());
        p.put("baseSap", c.getBaseSap());
        p.put("slUrl", c.getSlUrl());
        p.put("slUsuario", c.getSlUsuario());
        p.put("habilitada", c.getHabilitada());
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, c.getIdConexion() == null ? "I" : "U");
    }

    @Override
    public RespuestaSp guardarClave(int idConexion, String claveCifrada, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idConexion", idConexion);
        p.put("slClave", claveCifrada);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "C");
    }

    @Override
    public RespuestaSp eliminar(int idConexion, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idConexion", idConexion);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }
}
