package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Zona;

import java.util.List;

public interface IZona {

    /**
     * Procedimiento para obtener la Zona o residencia por ciudad
     * @param codCiudad
     * @return
     */
    List<Zona> obtenerZonaXCiudad(int codCiudad );

}
