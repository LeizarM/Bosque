package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.GrupoProduccion;


import java.util.List;

public interface IGrupoProduccion {


    /**
     * Para obtener los grupos de produccion por maquina
     * @return
     */
    List<GrupoProduccion> obtenerGrupoProduccion();

}
