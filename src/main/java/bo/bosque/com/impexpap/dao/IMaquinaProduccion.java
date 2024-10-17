package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.MaquinaProduccion;


import java.util.List;

public interface IMaquinaProduccion {


    /**
     * Procedimiento para listar las maquinas de producción
     * @return
     */
    List<MaquinaProduccion> obtenerMaquina();

}
