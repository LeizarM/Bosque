package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.dto.SolicitudPagoDto;
import bo.bosque.com.impexpap.model.SolicitudPago;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

public interface ISolicitudPago {


    /**
     * Procedimiento para registrar las Solicitudes de Pago
     *
     * @param mb
     * @param acc
     * @return
     */
    RespuestaSp registrarSolicitudPago(SolicitudPago mb, String acc);

    /**
     * Procedimiento para obtener las Solicitudes de Pago por Id
     *
     * @param idSolicitud
     * @return
     */
    List<SolicitudPago> obtenerSolicitudPago(long idSolicitud);

    /**
     * Carga UNA solicitud por su ID exacto (para load-before-update).
     * Solo envía @idSolicitud al SP; los demás params quedan en DEFAULT NULL.
     */
    SolicitudPago obtenerSolicitudPagoPorId(long idSolicitud);

    /**
     * Procedimiento para obtener las Solicitudes de Pago entre fechas
     *
     * @param fechaInicio
     * @param fechaFin
     * @return
     */
    List<SolicitudPagoDto> reporteSolicitudesXFecha(Date fechaInicio, Date fechaFin);


}