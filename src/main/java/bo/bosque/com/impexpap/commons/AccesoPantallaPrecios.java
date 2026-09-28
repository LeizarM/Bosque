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
 * El acceso a las pantallas del modulo de precios, del lado del servidor.
 *
 * <p><b>Por que existe.</b> En el JSF todo el modulo era una sola pantalla (vista 66):
 * quien no la tenia en el menu no podia tocar nada, ni catalogos ni porcentajes. Los
 * controladores nuevos solo exigian estar logueado ({@code ROLE_ADM} o {@code ROLE_LIM}),
 * y casi todos los usuarios son {@code ROLE_LIM}: cualquiera con sesion podia dar de
 * alta un color, cambiar el IVA o los porcentajes llamando al endpoint, aunque el menu
 * no le mostrara la pantalla. Esconder la pantalla en Flutter no autoriza nada.
 *
 * <p>Cada escritura exige ahora la pantalla donde vive, con la misma regla del menu:
 * {@code p_list_VistaUsuario 'M'}, las vistas con {@code nivelAcceso = 1} (quien no es
 * {@code lim} las tiene todas). Se compara por {@code direccion} y no por codVista
 * porque las seis vistas nuevas tienen otro codVista en cada base (166-171 en PRUEBA).
 * Los tres botones de la vista 66 se siguen exigiendo aparte, con
 * {@link AccesoModuloHelper#exigirBoton}.
 */
@Component
public class AccesoPantallaPrecios {

    private static final Logger log = LoggerFactory.getLogger(AccesoPantallaPrecios.class);

    public static final String PROPUESTAS = "tprAutorizacion/Autorizacion";
    public static final String FAMILIAS = "tprFamilias/Familias";
    public static final String PORCENTAJES = "tprPorcentajes/Porcentajes";
    public static final String CATALOGOS = "tprCatalogos/Catalogos";
    public static final String LISTAS = "tprListasPrecio/Listas";
    public static final String PARAMETROS = "tprParametros/Parametros";

    private final IVistaDao vistaDao;
    private final AccesoModuloHelper acceso;

    public AccesoPantallaPrecios(IVistaDao vistaDao, AccesoModuloHelper acceso) {
        this.vistaDao = vistaDao;
        this.acceso = acceso;
    }

    /** Corta con 403 si quien llama no tiene la pantalla habilitada. */
    public void exigir(Authentication auth, String direccion) {
        if (!tiene(auth, direccion)) {
            throw new AccessDeniedException("No tiene habilitada la pantalla '" + direccion + "'.");
        }
    }

    /** Falla cerrado: sin usuario o sin respuesta del procedimiento, no tiene acceso. */
    public boolean tiene(Authentication auth, String direccion) {
        // Igual que exigirBoton: el administrador no depende de tb_vistaUsuario.
        if (acceso.esAdmin(auth)) return true;

        int codUsuario = DatosToken.codUsuarioDe(auth);
        if (codUsuario <= 0) {
            log.warn("Pantallas de precios: el token no trae codUsuario; se niega '{}'", direccion);
            return false;
        }
        List<Vista> rutas = vistaDao.obtainRoutes(codUsuario);
        boolean tiene = rutas != null && rutas.stream()
                .anyMatch(v -> v.getDireccion() != null && direccion.equalsIgnoreCase(v.getDireccion().trim()));
        if (!tiene) {
            log.warn("Pantallas de precios: codUsuario={} sin la pantalla '{}'; se niega el acceso",
                    codUsuario, direccion);
        }
        return tiene;
    }
}
