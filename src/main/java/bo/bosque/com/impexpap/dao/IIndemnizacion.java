package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Indemnizacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;
import java.util.Map;

public interface IIndemnizacion {
    List<Indemnizacion> listarIndemnizacion(Indemnizacion indemnizacion);
    List<Indemnizacion> listarHistorialIndemnizacion(Indemnizacion indemnizacion);
    RespuestaSp abmIndemnizacion(Indemnizacion indemnizacion, String acc);
    List<Map<String, Object>> reporteProvision(String fechaCorte);
    Map<String, Object> obtenerDatosFiniquito(Integer codEmpleado, String fechaCorte);
}
