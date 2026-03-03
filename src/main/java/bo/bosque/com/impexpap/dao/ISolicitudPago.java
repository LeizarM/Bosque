package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.SolicitudPago;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ISolicitudPago {


    /**
     * Procedimiento para registrar las Solicitudes de Pago
     * @param mb
     * @param acc
     * @return
     */
    RespuestaSp registrarSolicitudPago(SolicitudPago mb, String acc );

    /**
     * Procedimiento para obtener las Solicitudes de Pago por Id
     * @param idSolicitud
     * @return
     */
    List<SolicitudPago> obtenerSolicitudPago( long idSolicitud );

}
