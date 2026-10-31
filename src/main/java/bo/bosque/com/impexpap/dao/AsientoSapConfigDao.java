package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.AsientoSapConfig;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Configuración del asiento de planilla en SAP por empresa ({@code tsap_AsientoPlanilla}). */
@Repository
public class AsientoSapConfigDao implements IAsientoSapConfig {

    private static final String SP_ABM = "p_abm_tsap_AsientoPlanilla";
    private static final String SP_LIST = "p_list_tsap_AsientoPlanilla";

    private final SpHelper spHelper;

    public AsientoSapConfigDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<AsientoSapConfig> listar() {
        return spHelper.ejecutarListado(SP_LIST, new HashMap<>(), "L", AsientoSapConfig.class);
    }

    @Override
    public AsientoSapConfig obtener(int codEmpresa) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codEmpresa", codEmpresa);
        List<AsientoSapConfig> filas = spHelper.ejecutarListado(SP_LIST, p, "L", AsientoSapConfig.class);
        return filas == null || filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public RespuestaSp guardar(AsientoSapConfig c, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codEmpresa", c.getCodEmpresa());
        p.put("habilitado", c.getHabilitado());
        p.put("baseSap", c.getBaseSap());
        p.put("linkedServer", c.getLinkedServer());
        p.put("esquema", c.getEsquema());
        p.put("sp", c.getSp());
        p.put("paramAnio", c.getParamAnio());
        p.put("paramMes", c.getParamMes());
        p.put("colCodigo", c.getColCodigo());
        p.put("colNombre", c.getColNombre());
        p.put("colCuenta", c.getColCuenta());
        p.put("colDebito", c.getColDebito());
        p.put("colCredito", c.getColCredito());
        p.put("prefijosSocio", c.getPrefijosSocio());
        p.put("diaDelMes", c.getDiaDelMes());
        p.put("memo", c.getMemo());
        p.put("referencia", c.getReferencia());
        p.put("campoReferencia", c.getCampoReferencia());
        p.put("evitarDuplicados", c.getEvitarDuplicados());
        p.put("tolerancia", c.getTolerancia());
        p.put("slUrl", c.getSlUrl());
        p.put("slUsuario", c.getSlUsuario());
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "G");
    }

    @Override
    public RespuestaSp guardarClave(int codEmpresa, String claveCifrada, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codEmpresa", codEmpresa);
        p.put("slClave", claveCifrada);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "C");
    }

    @Override
    public RespuestaSp eliminar(int codEmpresa, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codEmpresa", codEmpresa);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }
}
