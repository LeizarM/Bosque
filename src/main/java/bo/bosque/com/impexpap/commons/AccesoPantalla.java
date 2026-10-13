package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Exige en el servidor que quien llama tenga una pantalla en su menu.
 *
 * <p>Esconder la pantalla en Flutter no autoriza nada: casi todos los usuarios son
 * {@code ROLE_LIM} y un {@code ROLE_LIM} con sesion llama cualquier endpoint. La regla es la
 * del menu ({@code p_list_VistaUsuario 'M'} via {@link IVistaDao#obtainRoutes}): las vistas con
 * {@code nivelAcceso = 1}; el administrador las tiene todas.
 *
 * <p>Se compara por {@code tb_vista.direccion} y no por {@code codVista}, que es IDENTITY y cambia
 * de una base a otra. Falla cerrado: un token sin usuario da 403 y una respuesta nula del SP
 * se toma como "no tiene la pantalla".
 *
 * <p>Es la misma regla que ya aplican {@link AccesoPantallaPrecios} y el
 * {@code TareasRutinariasController}; esta es la version sin nombre de modulo, para los
 * controladores nuevos.
 */
@Component
public class AccesoPantalla {

    private static final Logger log = LoggerFactory.getLogger(AccesoPantalla.class);

    private final IVistaDao vistaDao;
    private final AccesoModuloHelper acceso;

    public AccesoPantalla(IVistaDao vistaDao, AccesoModuloHelper acceso) {
        this.vistaDao = vistaDao;
        this.acceso = acceso;
    }

    /** Corta con 403 si quien llama no tiene la pantalla habilitada. */
    public void exigir(Authentication auth, String direccion) {
        if (!tiene(auth, direccion)) {
            throw new AccessDeniedException("No tiene habilitada la pantalla '" + direccion + "'.");
        }
    }

    public boolean tiene(Authentication auth, String direccion) {
        if (direccion == null) return false;
        // El administrador no depende de tb_vistaUsuario, igual que en exigirBoton.
        if (acceso.esAdmin(auth)) return true;

        // Sin identidad valida en el token, codUsuarioDe ya corta con 403.
        int codUsuario = DatosToken.codUsuarioDe(auth);
        List<Vista> rutas = vistaDao.obtainRoutes(codUsuario);
        boolean tiene = rutas != null && rutas.stream()
                .anyMatch(v -> v.getDireccion() != null && direccion.equalsIgnoreCase(v.getDireccion().trim()));
        if (!tiene) {
            // Es lo que hay que buscar en el log cuando alguien dice "le di permisos y no entra".
            log.warn("ACL: codUsuario={} sin la pantalla '{}' en su menu; se niega el acceso",
                    codUsuario, direccion);
        }
        return tiene;
    }
}
