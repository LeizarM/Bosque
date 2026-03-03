package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.SolicitudProveedor;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ISolicitudProveedor {


    /**
     * Procedimiento para registrar las SolicitudesProveedor de Pago
     * @param mb
     * @param acc
     * @return
     */
    RespuestaSp registrarSolicitudProveedor( SolicitudProveedor mb , String acc );

    /**
     * Procedimiento para obtener las SolicitudesProveedor de Pago por Id
     * @param idSolicitudProveedor
     * @return
     */
    List<SolicitudProveedor> obtenerSolicitudProveedor( long idSolicitudProveedor );

    /**
     * Para obtener los proveedores SAP por empresa
     * @param codEmpresa
     * @return
     */
    List<SolicitudProveedor> obtenerProveedoresXEmpresa( int codEmpresa );


}
