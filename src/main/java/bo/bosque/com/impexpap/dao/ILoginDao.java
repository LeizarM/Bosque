package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Login;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;


public interface ILoginDao {

    /**
     * Para el abm del login
     * @param login
     * @return true si se realizo con exito
     */
    boolean abmLogin( Login login, String oper );

    /**
     * Para obtener el usuario y su estado (existe / bloqueado).
     *
     * <p>Ya NO recibe la contrasena: el SP no la usaba y mandarla en claro la
     * dejaba en las trazas del motor. Ver {@code LoginDaoImpl#verifyUser}.
     *
     * @param login nombre de usuario
     * @param ip    IP del cliente, para la bitacora del SP
     */
    Login verifyUser( String login, String ip );

    /**
     * Procedimiento para verificar los usuarios por nombre
     */
    UserDetails loadUserByUsername( String login );

    /**
     * Obtendra todos los usuarios
     * @return
     */
    List<Login> getAllUsers();

    /**
     * Registra un intento fallido de login y verifica si la cuenta debe ser bloqueada
     * @param login Nombre de usuario
     * @param ip Dirección IP del cliente
     * @return Login actualizado con información de intentos fallidos y estado
     */
    Login registerFailedAttempt(String login, String ip);

    /**
     * Registra un inicio de sesión exitoso en la bitácora
     * @param login Nombre de usuario
     * @param ip Dirección IP del cliente
     */
    void registerSuccessfulLogin(String login, String ip);

    /**
     * Verifica si el nombre de usuario ya existe por empleado
     * @param login
     * @return
     */
    int verifDuplicidad( Login login, String oper );
}
