package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IProgramador;
import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dto.MiEquipoDto;
import bo.bosque.com.impexpap.model.UsuarioBtn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <b>Quién sos y qué podés tocar</b>, resuelto en el servidor a partir del token.
 *
 * <p>Nació como los métodos privados de identidad de {@code RolSabadosController} — que sigue
 * usándolos, ahora delegando acá — y suma la pieza que faltaba: el gate contra el ACL de
 * botones ({@link #exigirBoton}), que es lo que cierra la deuda D4 del plan de migración.
 *
 * <h3>De dónde sale la identidad</h3>
 * Del token, y sólo del token. {@code JwtTokenFilter} deja en el {@code SecurityContext} un
 * {@code UserDetails} con <b>login + rol y nada más</b>: el {@code codUsuario} y el
 * {@code codEmpleado} viajan como claims del JWT pero el filtro no los copia a la
 * {@code Authentication}. Así que el puente {@code login → codUsuario} hay que hacerlo contra
 * la base, y se hace con el único que ya existe en Java:
 * {@code IProgramador.resolverPermiso(login)} → {@code p_list_trs_Programador @ACCION='E'},
 * que resuelve {@code SELECT TOP 1 codUsuario, codEmpleado FROM tb_usuario WHERE login=@login}
 * para cualquier login, sea o no programador.
 *
 * <p><b>Sí, eso acopla este helper al módulo de sábados</b>, y se eligió a conciencia: la
 * alternativa era un {@code SELECT} directo desde Java, que va contra la regla de oro del ERP
 * (toda la lógica en SPs). <b>TODO:</b> si el acoplamiento molesta, pedir al DBA una ACCION de
 * {@code p_list_Usuario} que resuelva login → codUsuario y cambiar sólo
 * {@link #miPermiso(Authentication)}.
 *
 * <h3>Las dos autorizaciones que viven acá NO son la misma</h3>
 * <ul>
 *   <li>{@link #exigirAdminORrhh(Authentication)} pregunta por {@code trs_Rrhh}, el padrón del
 *       módulo de SÁBADOS. No tiene nada que ver con el ACL de botones.</li>
 *   <li>{@link #exigirBoton(Authentication, int, String)} pregunta por
 *       {@code tb_usuarioBtn}/{@code tb_vistaBtn}, el ACL de toda la casa.</li>
 * </ul>
 * Están juntas porque las dos necesitan el mismo puente de identidad, no porque signifiquen lo
 * mismo. Un módulo nuevo casi siempre quiere la segunda.
 */
@Component
public class AccesoModuloHelper
{

    private static final Logger log = LoggerFactory.getLogger(AccesoModuloHelper.class);
 
    /** El puente {@code login → codUsuario / codEmpleado}. Ver el javadoc de la clase. */
    private final IProgramador programadorDao;
    /** El ACL de botones: {@code p_list_UsuarioBtn @ACCION='A'}. */
    private final IUsuarioBtn usuarioBtnDao;

    public AccesoModuloHelper(IProgramador programadorDao, IUsuarioBtn usuarioBtnDao) {
        this.programadorDao = programadorDao;
        this.usuarioBtnDao  = usuarioBtnDao;
    }

    // ==================================================================
    // IDENTIDAD
    // ==================================================================

    /**
     * Si el token trae {@code ROLE_ADM}.
     *
     * <p>Se lee de las authorities y no de la base: es lo mismo que evalúa
     * {@code @PreAuthorize}, así que no puede quedar desincronizado con el resto de la
     * seguridad. {@code ROLE_ADM} es exactamente el {@code tipoUsuario='adm'} de
     * {@code tb_usuario} — {@code p_list_Usuario @ACCION='B'} concede
     * {@code 'ROLE_'+UPPER(tipoUsuario)}.
     */
    public boolean esAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADM".equals(a.getAuthority()));
    }

    /**
     * El login del que está llamando.
     *
     * <p>Se usa {@code auth.getName()} y no un claim con el {@code codEmpleado} porque el
     * login es lo ÚNICO que el filtro JWT deja hoy en el {@code SecurityContext}: el principal
     * es el {@code UserDetails} que devuelve {@code loadUserByUsername}. El puente login →
     * codEmpleado lo hace {@code tb_usuario}, del lado del SQL.
     *
     * <p>Si esto viene vacío es que el token no se validó y el request llegó igual, así que se
     * corta acá antes de hacer nada.
     */
    public String loginDelToken(Authentication auth) {
        String login = auth != null ? auth.getName() : null;
        if (login == null || login.trim().isEmpty()) {
            throw new SpBusinessException("No se pudo identificar su usuario. "
                    + "Cierre sesión y vuelva a entrar.");
        }
        return login.trim();
    }

    /**
     * Quién soy, resuelto en el servidor.
     *
     * <p>Trae el permiso <b>sin</b> la lista de dependientes: los que escriben sólo necesitan
     * la identidad, y de la pertenencia al equipo ya se ocupa {@code trs_sp_programar} con su
     * EXISTS contra {@code fn_trs_ProgramadorDependiente()}. Cargar el equipo entero acá sería
     * una consulta al pedo en cada click.
     *
     * @return siempre un DTO, nunca {@code null}; con {@code esProgramador=0} si no lo es
     */
    public MiEquipoDto miPermiso(Authentication auth) {
        return programadorDao.resolverPermiso(loginDelToken(auth));
    }

    // ==================================================================
    // AUTORIZACIÓN
    // ==================================================================

    /**
     * Corta si quien llama no es ni {@code ROLE_ADM} ni RR.HH.
     *
     * <p><b>Es de rol-sabados</b>, no del ACL de botones: pregunta por {@code trs_Rrhh}, el
     * padrón de ese módulo. Un módulo nuevo casi seguro quiere
     * {@link #exigirBoton(Authentication, int, String)} en su lugar.
     *
     * <p><b>No es un {@code @PreAuthorize}</b> y no puede serlo: ser de RR.HH. no viaja en el
     * token, es una fila en {@code trs_Rrhh}. Por eso se resuelve contra la base, con la
     * identidad que sale del token. {@code esAdmin} va primero para que los 6 {@code ROLE_ADM}
     * no paguen esa consulta.
     */
    public void exigirAdminORrhh(Authentication auth) {
        if (esAdmin(auth)) return;
        if (miPermiso(auth).getEsRrhh() == 1) return;
        throw new AccessDeniedException("Solo Sistemas o RR.HH. administran estos permisos.");
    }

    /**
     * <b>SUPUESTO D4 — pendiente de confirmación de RR.HH. (ver plan §5).</b> El gate real
     * contra el ACL de botones que ya usa toda la casa ({@code tb_vistaBtn} +
     * {@code tb_usuarioBtn}), en vez de inventar un esquema de permisos nuevo.
     *
     * <p><b>Esconder el botón en el cliente no es autorizar.</b> El front ya envuelve las
     * acciones en {@code PermissionWidget}, pero eso sólo decide qué se dibuja: con Postman y
     * un token válido el endpoint queda igual de abierto. Este método es la puerta que sí
     * cierra.
     *
     * <h3>La identidad NUNCA sale del body</h3>
     * El {@code codUsuario} se resuelve desde el token, no se recibe. Ojo con el precedente
     * malo: {@code POST /view/vistaBtn} toma el {@code codUsuario} del JSON, o sea que
     * cualquiera puede pedir los permisos de otro. Acá no.
     *
     * <h3>El fallback de administrador es del legacy, no un invento</h3>
     * {@code Loggin.autorizarBtn()} (Bosque v2, línea 385) hace exactamente esto: si el botón
     * no aparece o tiene permiso 0, igual autoriza cuando {@code tipoUsuario.equals("adm")}.
     * Se replica para no cambiar en la migración quién puede qué. La única diferencia es el
     * ORDEN — acá el admin se resuelve primero, para ahorrarle dos consultas a los 6 usuarios
     * de Sistemas; el resultado es el mismo.
     *
     * <h3>Falla cerrado</h3>
     * Sin login, sin {@code codUsuario} o con el ACL vacío se responde 403, nunca "pasá igual".
     * Importa porque {@code UsuarioBtnDao} se anula a sí mismo el {@code JdbcTemplate} en su
     * {@code catch}: si alguna vez falla, el ACL devuelve lista vacía hasta que se reinicie la
     * app. Con esta política eso se ve como un 403 masivo y con un WARN en el log — molesto,
     * pero seguro. Al revés sería una puerta abierta silenciosa.
     *
     * <p><b>Verificación — ESTE GATE SÍ CAMBIA EL COMPORTAMIENTO, Y DE FORMA MASIVA. Anunciarlo
     * antes de desplegar.</b> Los 134 usuarios de {@code tb_usuario} tienen <i>fila</i> en
     * {@code tb_usuarioBtn} para estos botones, pero casi todas con {@code nivelAcceso = 0}, y
     * {@code p_list_UsuarioBtn @ACCION='A'} filtra {@code nivelAcceso != 0}. Medido contra
     * {@code BOSQUE-2_0} (vista 24): pasan <b>5</b> {@code btnDetalles}, <b>6</b>
     * {@code btnApoyoCalc}, <b>6</b> {@code btnReImprimirBoleta} y <b>4</b>
     * {@code btnReportesPYV}, más los <b>6</b> {@code ROLE_ADM} del fallback. O sea que el
     * módulo nuevo y los tres reportes quedan cerrados para ~123 personas que hoy entran.
     *
     * <p>Corolario práctico: los criterios #9 y #10 <b>no</b> necesitan borrar nada en una base
     * de prueba — se verifican con el token de cualquiera de esos ~123. Y {@code rramos}
     * ({@code tipoUsuario='lim'}) es uno de los 5 que <b>sí</b> tienen {@code btnDetalles} con
     * {@code nivelAcceso=1}, así que el criterio #9 se prueba tal cual está escrito.
     *
     * @param auth        el {@code Authentication} del request; el {@code codUsuario} sale de acá
     * @param codVista    {@code tb_vista.codVista} — 24 es "Permisos / Vacaciones / Abono Dias"
     * @param nombreBoton {@code tb_vistaBtn.nombreBtn}, p. ej. {@code "btnDetalles"}
     * @throws AccessDeniedException 403, con el mensaje fijo que pone {@code GlobalExceptionHandler}
     */
    public void exigirBoton(Authentication auth, int codVista, String nombreBoton) {
        if (!tieneBoton(auth, codVista, nombreBoton)) {
            throw new AccessDeniedException(
                    "No tiene habilitado '" + nombreBoton + "' en esta pantalla.");
        }
    }

    /**
     * Lo mismo que {@link #exigirBoton}, pero <b>respondiendo en vez de cortar</b>.
     *
     * <p>Existe para las autorizaciones que son un O: <i>"tenés el botón del ACL <b>o</b> el
     * recurso es tuyo"</i>. Es el caso de la boleta de vacación — el empleado tiene que poder
     * bajar la suya aunque RR.HH. le saque el botón de la consola. Con sólo la versión que tira
     * excepción, ese O habría que escribirlo con un try/catch alrededor de un throw, que es
     * exactamente el patrón que este helper vino a borrar.
     *
     * <p>Misma política que {@link #exigirBoton}: falla cerrado (devuelve {@code false}) y
     * loguea. Ver el javadoc de aquél para el detalle del ACL y del fallback de admin.
     */
    public boolean tieneBoton(Authentication auth, int codVista, String nombreBoton) {
        // Fallback de admin, igual que Loggin.autorizarBtn() del legacy.
        if (esAdmin(auth)) return true;

        Long codUsuario = miPermiso(auth).getCodUsuario();
        if (codUsuario == null) {
            log.warn("ACL: el login '{}' no resuelve a ningun codUsuario de tb_usuario; se niega {}/{}",
                    loginDelToken(auth), codVista, nombreBoton);
            return false;
        }

        // ACCION 'A' ya filtra nivelAcceso != 0, pero se vuelve a comprobar: el gate no debe
        // depender de un WHERE que vive en otro archivo.
        //
        // anyMatch y NO "esperar una unica fila": tb_vistaBtn tiene nombres DUPLICADOS
        // (btnImprimirSolicitud en codBtn 111 y 115, btnNuevaVacPagada en 110 y 114), asi que
        // un lookup por nombre puede devolver 2 filas legitimamente.
        List<UsuarioBtn> botones = usuarioBtnDao.botonesXUsuario(codUsuario.intValue());
        boolean autorizado = botones != null && botones.stream()
                .anyMatch(b -> b.getPertenVist() == codVista
                            && nombreBoton.equals(b.getBoton())
                            && b.getPermiso() != 0);

        if (!autorizado) {
            log.warn("ACL: codUsuario={} sin el boton '{}' de la vista {}; se niega el acceso",
                    codUsuario, nombreBoton, codVista);
        }
        return autorizado;
    }

    /**
     * El {@code codEmpleado} de quien está llamando, resuelto desde el token.
     *
     * <p>Es la otra mitad del O de {@link #tieneBoton}: sirve para preguntar "¿este recurso es
     * suyo?". Puede venir {@code null} si el login no está atado a ningún empleado en
     * {@code tb_usuario} — usuarios de sistema, por ejemplo—; quien lo use tiene que tratar ese
     * {@code null} como "no es suyo", nunca como "coincide".
     */
    public Long codEmpleadoDelToken(Authentication auth) {
        return miPermiso(auth).getCodEmpleado();
    }
}
