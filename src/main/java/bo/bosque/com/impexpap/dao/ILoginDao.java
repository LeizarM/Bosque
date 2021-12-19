package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Login;

public interface ILoginDao {

    /**
     * Para el abm del login
     * @param login
     * @return
     */
    boolean abmLogin( Login login );

    /**
     * Para obtener el usuario
     * @param login
     * @param password
     * @return
     */
    Login obtainUser( String login, String password );
}
