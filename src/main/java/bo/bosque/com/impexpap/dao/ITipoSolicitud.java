package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TipoSolicitud;

import java.util.List;

public interface ITipoSolicitud {


    /**
     * Obtiene todos los tipos de solicitud registrados en la base de datos.
     * @return
     */
    List<TipoSolicitud> obtenerTipoSolicitudes();
}
