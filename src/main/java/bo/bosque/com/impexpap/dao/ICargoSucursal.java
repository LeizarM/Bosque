package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CargoSucursal;

import java.util.List;

public interface ICargoSucursal {


    /**
     * Procedimiento para registrar un nuevo cargo por sucursal
     * @param mb
     * @return
     */
    boolean registrarCargoSucursal( CargoSucursal mb, String acc );

    /**
     * Procedimiento para obtener los cargos por Sucursal
     * @param codSucursal
     * @return
     */
    List<CargoSucursal> obtenerSucursalesXEmpresa( int codSucursal );

    /**
     * Obtendra un cargo por sucursal
     * @return
     */
    List<CargoSucursal> obtenerCargoEnSucursales( int codCargo );
}
