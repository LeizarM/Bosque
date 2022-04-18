package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Licencia;

import java.util.List;

public interface ILicencia {

    /**
     * Procedimiento para obtener las licencia de conducir de una persona
     * @param codPersona
     * @return
     */
    List<Licencia> obtenerLicencia(int codPersona );
}
