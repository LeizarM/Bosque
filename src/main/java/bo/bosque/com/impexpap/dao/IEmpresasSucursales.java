package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.model.Empresa;
import bo.bosque.com.impexpap.model.Sucursal;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Mantenimiento de empresas y sucursales (pantalla de estructura organizacional) por
 * {@code p_list_Empresa}/{@code p_list_Sucursal} ACCION 'P' y sus {@code p_abm_}.
 */
public interface IEmpresasSucursales {

    List<Empresa> listarEmpresas();

    /** Las sucursales de una empresa, o todas con {@code codEmpresa} 0. */
    List<Sucursal> listarSucursales(long codEmpresa);

    /** 'I' si {@code codEmpresa} es 0, 'U' si es mayor. */
    RespuestaSp registrarEmpresa(Empresa empresa);

    /** 'I' si {@code codSucursal} es 0, 'U' si es mayor. */
    RespuestaSp registrarSucursal(Sucursal sucursal);

    RespuestaSp eliminarEmpresa(long codEmpresa, int audUsuario);

    RespuestaSp eliminarSucursal(long codSucursal, int audUsuario);
}
