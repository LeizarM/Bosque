package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CcrSolicitud;

import java.util.Date;
import java.util.List;

public interface ICcrSolicitud {

    /**
     * Solicitudes de corte de un rango de fechas.
     * Con ambas fechas en null devuelve todas.
     * @param fechaIni
     * @param fechaFin
     * @return
     */
    List<CcrSolicitud> obtenerSolicitudes( Date fechaIni, Date fechaFin );

    /**
     * Registra la cabecera y devuelve el idSolicitud generado, o 0 si fallo.
     * @param solicitud
     * @return
     */
    long registrarSolicitud( CcrSolicitud solicitud );

    /**
     * Cancela una solicitud: deja estado en 'CNC' con el motivo.
     * @param solicitud con idSolicitud, observacion y audUsuario
     * @return
     */
    boolean cancelarSolicitud( CcrSolicitud solicitud );

    /**
     * Borra la cabecera. Solo se usa para deshacer un alta que quedo a medias.
     * @param idSolicitud
     * @return
     */
    boolean eliminarSolicitud( long idSolicitud );

}
