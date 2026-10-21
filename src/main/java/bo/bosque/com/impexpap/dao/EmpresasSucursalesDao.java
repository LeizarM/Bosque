package bo.bosque.com.impexpap.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.model.Empresa;
import bo.bosque.com.impexpap.model.Sucursal;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;

/**
 * Acceso a {@code tb_empresa} y {@code tb_sucursal} vía {@link SpHelper}. Los DAO viejos
 * ({@code EmpresaDao}, {@code SucursalDao}) siguen sirviendo a las demás pantallas.
 */
@Repository
public class EmpresasSucursalesDao implements IEmpresasSucursales {

    private final SpHelper spHelper;

    public EmpresasSucursalesDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<Empresa> listarEmpresas() {
        return spHelper.ejecutarListado("p_list_Empresa", new LinkedHashMap<String, Object>(), "P", Empresa.class);
    }

    @Override
    public List<Sucursal> listarSucursales(long codEmpresa) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (codEmpresa > 0) p.put("codEmpresa", codEmpresa);
        return spHelper.ejecutarListado("p_list_Sucursal", p, "P", Sucursal.class);
    }

    @Override
    public RespuestaSp registrarEmpresa(Empresa e) {
        boolean alta = e.getCodEmpresa() == 0;
        Map<String, Object> p = new LinkedHashMap<>();
        if (!alta) p.put("codEmpresa", e.getCodEmpresa());
        p.put("nombre", e.getNombre());
        p.put("codPadre", e.getCodPadre());
        p.put("audUsuarioI", e.getAudUsuario());
        return spHelper.ejecutarAbmMap("p_abm_Empresa", p, alta ? "I" : "U");
    }

    @Override
    public RespuestaSp registrarSucursal(Sucursal s) {
        boolean alta = s.getCodSucursal() == 0;
        Map<String, Object> p = new LinkedHashMap<>();
        if (!alta) p.put("codSucursal", s.getCodSucursal());
        p.put("nombre", s.getNombre());
        p.put("codEmpresa", s.getCodEmpresa());
        p.put("codCiudad", s.getCodCiudad());
        p.put("audUsuarioI", s.getAudUsuarioI());
        return spHelper.ejecutarAbmMap("p_abm_Sucursal", p, alta ? "I" : "U");
    }

    @Override
    public RespuestaSp eliminarEmpresa(long codEmpresa, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codEmpresa", codEmpresa);
        p.put("audUsuarioI", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_Empresa", p, "D");
    }

    @Override
    public RespuestaSp eliminarSucursal(long codSucursal, int audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codSucursal", codSucursal);
        p.put("audUsuarioI", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_Sucursal", p, "D");
    }
}
