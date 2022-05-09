package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CargoSucursal;

import java.util.List;

public interface ICargoSucursal {

    /**
     * Procedimiento para obtener los cargos por Sucursal
     * @param codSucursal
     * @return
     */
    List<CargoSucursal> obtenerSucursalesXEmpresa( int codSucursal );
}
