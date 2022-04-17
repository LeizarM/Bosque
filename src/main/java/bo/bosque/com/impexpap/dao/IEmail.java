package bo.bosque.com.impexpap.dao;

import java.util.List;
import bo.bosque.com.impexpap.model.Email;


public interface IEmail {



    /**
     * Procedimiento que obtendra los correos por persona
     */
    List<Email> obtenerCorreos(int codPersona );
}
