package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Sucursal;

import java.util.List;

public interface ISucursal {

    /**
     * Registrar una sucursal por empresa
     * @param mb
     * @param acc
     * @return
     */
    boolean registrarSucursal( Sucursal mb, String acc );


    /**
     * obtendra las sucursales por empresa
     * @param codEmpresa
     * @return
     */
    List<Sucursal> obtenerSucursalesXEmpresa (int codEmpresa );
}
