package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Sucursal;

import java.util.List;

public interface ISucursal {


    /**
     * obtendra las sucursales por empresa
     * @param codEmpresa
     * @return
     */
    List<Sucursal> obtenerSucursalesXEmpresa (int codEmpresa );
}
