package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.model.Ciudad;
import bo.bosque.com.impexpap.model.Pais;
import bo.bosque.com.impexpap.model.Zona;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Mantenimiento de países, ciudades y zonas (pantalla de ubicación geográfica) por sus
 * {@code p_list_*} ACCION 'P' y sus {@code p_abm_*}. Las reglas y los mensajes los ponen los SP.
 */
public interface IUbicacionGeografica {

    List<Pais> listarPaises();

    /** Las ciudades de un país, o todas con {@code codPais} 0. */
    List<Ciudad> listarCiudades(long codPais);

    /** Las zonas de una ciudad, o todas con {@code codCiudad} 0. */
    List<Zona> listarZonas(long codCiudad);

    RespuestaSp registrarPais(Pais pais);

    RespuestaSp registrarCiudad(Ciudad ciudad);

    RespuestaSp registrarZona(Zona zona);

    RespuestaSp eliminarPais(long codPais, int audUsuario);

    RespuestaSp eliminarCiudad(long codCiudad, int audUsuario);

    RespuestaSp eliminarZona(long codZona, int audUsuario);
}
