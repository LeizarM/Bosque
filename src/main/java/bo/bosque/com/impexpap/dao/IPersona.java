package bo.bosque.com.impexpap.dao;


import bo.bosque.com.impexpap.model.Persona;
import bo.bosque.com.impexpap.utils.Tipos;

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
    Integer registrarPersona ( Persona persona, String acc);

    /**
     * Procedimiento para obtener el ultimo codigo de la persona insertada
     * @return
     */
    int obtenerUltimoPersona();
    /**
     * Procedimiento para obtener el tipo de sexo
     * @return
     */
    List<Tipos> lstSexo ();
    /**
     * Procedimiento para obtener el tipo de sexo
     * @return
     */
    List<Tipos>lstCiExp();
    /**
     * Procedimiento para obtener el tipo de sexo
     * @return
     */
    List<Tipos>lstEstadoCivil();
    /**
     * Procedimiento para obtener el tipo de formacion
     * @return
     */
    List<Tipos>lstTipoFormacion();
    /**
     * Procedimiento para obtener lista de duracion formacion
     * @return
     */
    List<Tipos>lstTipoDuracionFormacion();

    /**
     * Procedimiento para obtener una lista de personas
     * @return
     */
    List<Persona>obtenerListaPersonas();
    /**
     * Procedimiento para obtener datos personales de un empleado
     * @param codPersona
     * @return
     */
    Persona obtenerDatosXCarnet (String ciNumero );


}
