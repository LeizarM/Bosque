package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Cotizaciones;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface ICotizaciones {

    /**
     * Registrar, actualizar o eliminar una Cotización
     * @param mb   Objeto con los datos
     * @param acc  Acción ('I', 'U', 'D')
     */
    RespuestaSp registrarCotizaciones(Cotizaciones mb, String acc);

    /**
     * Obtener cotizaciones por ID. Si idCotizacion == 0 devuelve todas.
     * @param idCotizacion ID a buscar (0 = todas)
     */
    List<Cotizaciones> obtenerCotizaciones(long idCotizacion);

    /**
     * Obtener cotizaciones asociadas a una solicitud
     * @param idSolicitud ID de la solicitud
     */
    List<Cotizaciones> obtenerCotizacionesPorSolicitud(long idSolicitud);
}

