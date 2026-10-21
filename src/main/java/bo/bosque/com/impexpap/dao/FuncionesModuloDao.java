package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.OpcionDto;
import bo.bosque.com.impexpap.model.AsigFunModXEmpl;
import bo.bosque.com.impexpap.model.FuncionesXModulo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/** Acceso a {@code tb_funcionesXModulo} y {@code tb_asigFunModXEmpl} vía {@link SpHelper}. */
@Repository
public class FuncionesModuloDao implements IFuncionesModulo {

    private static final String SP_LIST_FUNC = "p_list_FuncionesXModulo";
    private static final String SP_ABM_FUNC = "p_abm_FuncionesXModulo";
    private static final String SP_LIST_ASIG = "p_list_AsigFunModXEmpl";
    private static final String SP_ABM_ASIG = "p_abm_AsigFunModXEmpl";

    private final SpHelper spHelper;

    public FuncionesModuloDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<OpcionDto> modulos() {
        return spHelper.ejecutarListado(SP_LIST_FUNC, new LinkedHashMap<String, Object>(), "M", OpcionDto.class);
    }

    @Override
    public List<FuncionesXModulo> listarFunciones(long codModulo) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (codModulo > 0) p.put("codModulo", codModulo);
        return spHelper.ejecutarListado(SP_LIST_FUNC, p, "P", FuncionesXModulo.class);
    }

    @Override
    public RespuestaSp registrarFuncion(FuncionesXModulo f) {
        boolean alta = f.getCodFuncMod() == 0;
        Map<String, Object> p = new LinkedHashMap<>();
        if (!alta) p.put("codFuncMod", f.getCodFuncMod());
        p.put("codModulo", f.getCodModulo());
        p.put("funcion", f.getFuncion());
        p.put("detalle", f.getDetalle());
        p.put("requerido", f.getRequerido());
        p.put("estado", f.getEstado());
        p.put("audUsuario", f.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM_FUNC, p, alta ? "I" : "U");
    }

    @Override
    public RespuestaSp eliminarFuncion(long codFuncMod, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codFuncMod", codFuncMod);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM_FUNC, p, "D");
    }

    @Override
    public List<AsigFunModXEmpl> listarAsignaciones(long codModulo, long codFuncMod, long codSucursal, String estado) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (codModulo > 0) p.put("codModulo", codModulo);
        if (codFuncMod > 0) p.put("codFuncMod", codFuncMod);
        if (codSucursal > 0) p.put("codSucursal", codSucursal);
        if (estado != null && !estado.trim().isEmpty()) p.put("estado", estado.trim());
        return spHelper.ejecutarListado(SP_LIST_ASIG, p, "P", AsigFunModXEmpl.class);
    }

    @Override
    public List<OpcionDto> sucursales() {
        return spHelper.ejecutarListado(SP_LIST_ASIG, new LinkedHashMap<String, Object>(), "S", OpcionDto.class);
    }

    @Override
    public List<OpcionDto> empleadosDeSucursal(long codSucursal) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codSucursal", codSucursal);
        return spHelper.ejecutarListado(SP_LIST_ASIG, p, "E", OpcionDto.class);
    }

    @Override
    public RespuestaSp registrarAsignacion(AsigFunModXEmpl a) {
        boolean alta = a.getCodAsignacion() == 0;
        Map<String, Object> p = new LinkedHashMap<>();
        if (alta) {
            p.put("codFuncMod", a.getCodFuncMod());
            p.put("codEmpleado", a.getCodEmpleado());
            p.put("codSucursal", a.getCodSucursal());
        } else {
            p.put("codAsignacion", a.getCodAsignacion());
        }
        p.put("estado", a.getEstado());
        p.put("audUsuario", a.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM_ASIG, p, alta ? "I" : "U");
    }
}
