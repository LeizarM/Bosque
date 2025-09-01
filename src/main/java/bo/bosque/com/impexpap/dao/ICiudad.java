package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Ciudad;

import java.util.List;

public interface ICiudad {

    /**
     * Obtendra la lista de Ciudades por Pais
     * @param codPais
     * @return
     */
    List<Ciudad> obtenerCiudadesXPais ( int codPais );
    /**
     * Procedimiento para registrar pais
     * @param
     * @param
     * @return
     */
    boolean registrarCiudad(Ciudad ciudad, String acc);
}
