package bo.bosque.com.impexpap.dao;

import java.util.List;
import bo.bosque.com.impexpap.model.Email;


public interface IEmail {


    /**
     * Procedimiento que obtendra los correos por persona
     * @param codPersona
     * @return
     */
    List<Email> obtenerCorreos(int codPersona );

    /**
     * Procedimiento para registrar los emails
     * @param email
     * @param acc
     * @return
     */
    boolean registrarEmail( Email email, String acc);


    /**
     * Procedimiento para obtener el ultimo codigo de persona
     * @param audUsuario
     * @return
     */
    int obtenerUltimoCodPersona( int audUsuario );
}
