package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Cotizaciones;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CotizacionesDao implements ICotizaciones {

    private final SpHelper spHelper;

    public CotizacionesDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrarCotizaciones(Cotizaciones mb, String acc) {
        return spHelper.ejecutarAbm("p_abm_tpex_Cotizaciones", mb, acc);
    }

    @Override
    public List<Cotizaciones> obtenerCotizaciones(long idCotizacion) {
        Cotizaciones filtro = new Cotizaciones();
        filtro.setIdCotizacion(idCotizacion);
        return spHelper.ejecutarListado("p_list_tpex_Cotizaciones", filtro, "L", Cotizaciones.class);
    }

    @Override
    public List<Cotizaciones> obtenerCotizacionesPorSolicitud(long idSolicitud) {
        Cotizaciones filtro = new Cotizaciones();
        filtro.setIdSolicitud(idSolicitud);

        return spHelper.ejecutarListado("p_list_tpex_Cotizaciones", filtro, "B", Cotizaciones.class);
    }
}

