package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Pantallas del módulo ({@code tb_vista.direccion}, contrato 3.7) y la regla de acceso del evaluador: ocultarlas en
 * Flutter no autoriza nada, casi todos son {@code ROLE_LIM}. Es la regla de {@link AccesoPantalla}, pero acepta
 * cualquiera de varias pantallas en una consulta; {@code MisExamenes} no se exige (solo devuelve lo propio).
 */
@Component
public class TcpePermisos {

    private static final Logger log = LoggerFactory.getLogger(TcpePermisos.class);

    public static final String BANCO = "tcpeExamenes/Banco";
    public static final String LOTES = "tcpeExamenes/Lotes";
    public static final String RESULTADOS = "tcpeExamenes/Resultados";
    public static final String INFORMES = "tcpeExamenes/Informes";
    public static final String USUARIOS = "tcpeExamenes/Usuarios";
    public static final String MIS_EXAMENES = "tcpeExamenes/MisExamenes";
    /** Panel "En vivo" del evaluador (contrato 8). */
    public static final String EN_VIVO = "tcpeExamenes/EnVivo";
    /** Gestión de las preguntas de la semana (contrato 15.3). Responderlas, desde el inicio, no la pide. */
    public static final String PREGUNTA_SEMANA = "tcpeExamenes/PreguntaSemana";

    /** Cualquier pantalla de evaluador: alcanza para leer y mantener los catálogos (categorías incluidas). */
    public static final String[] EVALUADOR = { BANCO, LOTES, RESULTADOS, INFORMES, USUARIOS, EN_VIVO, PREGUNTA_SEMANA };

    private final IVistaDao vistaDao;
    private final AccesoModuloHelper acceso;

    public TcpePermisos(IVistaDao vistaDao, AccesoModuloHelper acceso) {
        this.vistaDao = vistaDao;
        this.acceso = acceso;
    }

    /** Corta con 403 si quien llama no tiene ninguna de esas pantallas en su menú. */
    public void exigirAlguna(Authentication auth, String... direcciones) {
        if (acceso.esAdmin(auth)) return;
        int codUsuario = DatosToken.codUsuarioDe(auth);
        List<Vista> rutas = vistaDao.obtainRoutes(codUsuario);
        List<String> pedidas = Arrays.asList(direcciones);
        boolean tiene = rutas != null && rutas.stream().anyMatch(v -> v.getDireccion() != null
                && pedidas.stream().anyMatch(d -> d.equalsIgnoreCase(v.getDireccion().trim())));
        if (!tiene) {
            log.warn("ACL exámenes: codUsuario={} sin ninguna de las pantallas {}", codUsuario, pedidas);
            // SinPermisoException (403) y no AccessDeniedException a secas: GlobalExceptionHandler cambia el
            // mensaje de esta última por un texto fijo y el usuario no sabría qué pantalla le falta.
            throw new SinPermisoException("No tiene habilitada la pantalla del módulo de exámenes para esta acción.");
        }
    }
}
