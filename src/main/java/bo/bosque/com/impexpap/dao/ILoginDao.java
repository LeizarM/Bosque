package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Login;
import org.springframework.security.core.userdetails.UserDetails;


public interface ILoginDao {

    /**
     * Para el abm del login
     * @param login
     * @return true si se realizo con exito
     */
    boolean abmLogin( Login login, String oper );

    /**
     * Para obtener el usuario
     * @param login
     * @param password
     * @return
     */
    Login verifyUser( String login, String password, String ip );

    /**
     * Procedimiento para verificar los usuarios por nombre
     */
    UserDetails loadUserByUsername( String login );
}
