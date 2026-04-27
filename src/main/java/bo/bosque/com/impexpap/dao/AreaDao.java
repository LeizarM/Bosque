package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.Area;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AreaDao implements IArea {
    private final SpHelper spHelper;

    public AreaDao (SpHelper spHelper){this.spHelper = spHelper;}

    /**
     *
     * @param a
     * @return
     */
    @Override
    public List<Area> obtenerArea(Area a){
        return this.spHelper.ejecutarListado("p_list_Area",a,"L",Area.class);
    }

    /**
     * REGISTRO AREA
     * @param a
     * @param acc
     * @return
     */
    @Override
    public RespuestaSp registrarArea(Area a, String acc){
        return this.spHelper.ejecutarAbm("p_abm_Area",a,acc);
    }
}
