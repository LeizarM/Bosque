package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.DetalleSolicitud;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IDetalleSolicitud {


    /**
     * Procedimiento para registrar las Detalle de Solicitudes Proveedor
     * @param mb
     * @param acc
     * @return
     */
    RespuestaSp registrarDetalleSolicitud( DetalleSolicitud  mb , String acc );

    /**
     * Procedimiento para obtener las DetalleSolicitud de Pago por Id
     * @param idDetalle
     * @return
     */
    List<DetalleSolicitud> obtenerDetalleSolicitud(long idDetalle );

    /**
     * Procedimiento para obtener las ordenes de compra por empresa y facturas proveedor
     * @param codEmpresa
     * @return
     */
    List<DetalleSolicitud> obtenerFacProvYOrdCompraXEmpresa( int codEmpresa );

}
