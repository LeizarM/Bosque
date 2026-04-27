package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Anticipo;
import bo.bosque.com.impexpap.model.Area;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

public interface IArea {
    /**
     *obtiene lista de areas
     * @param a
     * @return
     */
    List<Area> obtenerArea(Area a);

    /**
     * registro area
     * @param a
     * @param acc
     * @return
     */
    RespuestaSp registrarArea (Area a, String acc);
}
