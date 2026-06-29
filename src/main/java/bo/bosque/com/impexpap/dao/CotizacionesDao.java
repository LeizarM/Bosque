package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Cotizaciones;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // ── R: registro individual (formulario) ───────────────────────────────
    // Map: el modelo enviaría idSolicitud/codBanco en 0 y el SP filtraría
    // con esos ceros (lista vacía). Misma regla que AsientosDao.
    @Override
    public List<Cotizaciones> obtenerCotizaciones(long idCotizacion) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idCotizacion", idCotizacion);
        return spHelper.ejecutarListado("p_list_tpex_Cotizaciones", filtro, "R", Cotizaciones.class);
    }

    // ── L: cotizaciones de una solicitud (comparativa) ────────────────────
    // ACCION "L" devuelve codBanco + banco; la "B" (reporte por fechas) no
    // incluye codBanco y el frontend mostraba "Banco #0".
    @Override
    public List<Cotizaciones> obtenerCotizacionesPorSolicitud(long idSolicitud) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idSolicitud", idSolicitud);
        return spHelper.ejecutarListado("p_list_tpex_Cotizaciones", filtro, "L", Cotizaciones.class);
    }
}

