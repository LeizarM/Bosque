package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Persona;

import java.util.List;

public interface IPersona {

    /**
     * Procedimiento para obtener datos personales de un empleado
     * @param codPersona
     * @return
     */
    Persona obtenerDatosPersonales (int codPersona );

    /**
     * Procedimiento para registrar una persona
     * @param persona
     * @param acc
     */
    boolean registrarPersona ( Persona persona, String acc);

    /**
     * Procedimiento para obtener el ultimo codigo de la persona insertada
     * @return
     */
    int obtenerUltimoPersona();

}
