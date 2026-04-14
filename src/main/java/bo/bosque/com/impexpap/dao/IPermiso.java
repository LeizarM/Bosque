package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Permiso;
import bo.bosque.com.impexpap.model.TigoEjecutado;

import java.util.List;

public interface IPermiso {
    /**
     * OBTENDRA LOS DIAS TOTALES DISPONIBLES DE VACACION
     */
    List<Permiso> diasDisponibles(Permiso filtro);
}
