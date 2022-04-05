package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Persona;
public interface IPersona {

    /**
     * Procedimiento para obtener datos personales de un empleado
     */
    Persona obtenerDatosPersonales (int codPersona );

}
