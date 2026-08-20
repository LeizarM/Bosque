package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CcrSolicitudDetalle;

import java.util.List;

public interface ICcrSolicitudDetalle {

    /**
     * Los items de una solicitud, con lo que devolvio SAP.
     * @param idSolicitud
     * @return
     */
    List<CcrSolicitudDetalle> obtenerDetalleXSolicitud( long idSolicitud );

    /**
     * Registra un item de la solicitud.
     * @param detalle
     * @return
     */
    boolean registrarDetalle( CcrSolicitudDetalle detalle );

    /**
     * Borra todos los items de una solicitud (ACCION 'E').
     * Se usa para deshacer un alta que quedo a medias.
     * @param idSolicitud
     * @return
     */
    boolean eliminarDetalleXSolicitud( long idSolicitud );

}
